package indigo

private const val MAX_DECK_SIZE = 52

class GameOverException(message: String) : Exception(message)

data class Card(val rank: String, val suit: String) {
    override fun toString(): String {
        return "$rank$suit"
    }
}

open class Deck {
    var cards: List<Card>

    constructor() {
        cards = mutableListOf()
    }

    private constructor(cards: List<Card>) {
        this.cards = cards
    }

    fun popCards(n: Int): Deck {
        if (n !in 1..MAX_DECK_SIZE) {
            throw IllegalArgumentException("Invalid number of cards.")
        }

        var cardsToPop = n
        if (n > cards.size) {
            cardsToPop = cards.size
        }

        val result = cards.subList(0, cardsToPop)
        cards = cards.subList(cardsToPop, cards.size)
        return Deck(result)
    }

    fun popRemainingCards(): Deck = popCards(cards.size)

    fun sameSuiteCardIndicesOrNull(): List<Int>? {
        val suiteToIndicesMap = cards.groupBy(keySelector = { it.suit }, valueTransform = { cards.indexOf(it) })
        return suiteToIndicesMap.values.firstOrNull { it.size > 1 }
    }

    fun sameRankCardIndicesOrNull(): List<Int>? {
        val rankToIndicesMap = cards.groupBy(keySelector = { it.rank }, valueTransform = { cards.indexOf(it) })
        return rankToIndicesMap.values.firstOrNull { it.size > 1 }
    }

    fun retrieveRandomCard(cardIndices: Collection<Int>): Card = retrieveCard(cardIndices.random())

    fun retrieveCard(n: Int): Card {
        val result = cards[n]

        val newCards = cards.toMutableList()
        newCards.removeAt(n)
        cards = newCards

        return result
    }

    fun putCard(card: Card) {
        cards = cards.plus(card).toMutableList()
    }

    fun putCards(deck: Deck) {
        cards = cards.plus(deck.cards).toMutableList()
    }

    fun size(): Int = cards.size

    fun topCard(): Card? = cards.lastOrNull()

    fun penultimate(): Card = cards[cards.lastIndex - 1]

    fun isNotEmpty(): Boolean = cards.isNotEmpty()

    override fun toString(): String = cards.joinToString(separator = " ")
}

class MainDeck : Deck() {

    init {
        this.cards = buildCards()
    }

    private fun buildCards(): MutableList<Card> {
        val ranksList = "A 2 3 4 5 6 7 8 9 10 J Q K".split(" ").toList()
        val suitsList = "♦ ♥ ♠ ♣".split(" ").toList()

        val deck = mutableListOf<Card>()
        for (suite in suitsList) {
            deck.addAll(ranksList.map { rank -> Card(rank, suite) })
        }

        return deck.shuffled().toMutableList()
    }
}

open class Player {
    val playerHand: Deck = Deck()
    val wonCards: Deck = Deck()

    fun hasCardsInHand(): Boolean = playerHand.isNotEmpty()

    fun takeCardsToHand(deck: Deck) {
        playerHand.putCards(deck)
    }

    fun takeWonCards(deck: Deck) {
        wonCards.putCards(deck)
    }

    fun wonAnyCards(): Boolean = wonCards.isNotEmpty()

    open fun makeTurn(tableDeck: Deck) {
        val deckMenu =
            playerHand.toString().split(" ")
                .mapIndexed { index, s -> "${index + 1})$s" }
                .joinToString(separator = " ")

        println("Cards in hand: $deckMenu")

        while (true) {
            println("Choose a card to play (1-${playerHand.size()}):")

            val n = try {
                val userInput = readln()

                if (userInput == "exit") {
                    throw GameOverException("Game Over")
                }

                userInput.toInt()
            } catch (e: NumberFormatException) {
                continue
            }

            tableDeck.putCard(
                when (n) {
                    in 1..playerHand.size() -> playerHand.retrieveCard(n - 1)
                    else -> continue
                }
            )

            break
        }
    }
}

class ComputerPlayer : Player() {
    private lateinit var candidateCardsIndices: Map<String, List<Int>>

    override fun makeTurn(tableDeck: Deck) {
        println(playerHand)
        updateCandidateCardsIndices(tableDeck)

        when {
            oneCardInHand() -> {
                val card = playerHand.retrieveCard(0)
                println("Computer plays $card")
                tableDeck.putCard(card)
            }

            noCandidateCards() -> {
                tableDeck.putCard(
                    (playerHand.sameSuiteCardIndicesOrNull()?.let {
                        playerHand.retrieveRandomCard(it)
                    } ?: playerHand.sameRankCardIndicesOrNull()?.let {
                        playerHand.retrieveRandomCard(it)
                    }
                    ?: playerHand.retrieveRandomCard(playerHand.cards.indices.toList())).also { println("Computer plays $it") }
                )
            }

            oneCandidateCard() -> {
                val card = playerHand.retrieveCard(candidateCardsIndices.values.first { it.isNotEmpty() }[0])
                println("Computer plays $card")
                tableDeck.putCard(card)
            }

            multipleCandidateCards() -> {
                val candidateCardsWithSameSuit = candidateCardsIndices["same_suite"]!!
                if (candidateCardsWithSameSuit.size > 1) {
                    val card = playerHand.retrieveRandomCard(candidateCardsWithSameSuit)
                    println("Computer plays $card")
                    tableDeck.putCard(card)
                    return
                }

                val candidateCardsWithSameRank = candidateCardsIndices["same_rank"]!!
                if (candidateCardsWithSameRank.size > 1) {
                    val card = playerHand.retrieveRandomCard(candidateCardsWithSameRank)
                    println("Computer plays $card")
                    tableDeck.putCard(card)
                    return
                }

                val allCandidateCards = candidateCardsWithSameSuit + candidateCardsWithSameRank
                val card = playerHand.retrieveRandomCard(allCandidateCards.toSet())
                println("Computer plays $card")
                tableDeck.putCard(card)
            }
        }
    }

    private fun oneCardInHand() = playerHand.cards.size == 1

    private fun noCandidateCards() = countCandidateCards() == 0

    private fun oneCandidateCard() = countCandidateCards() == 1

    private fun multipleCandidateCards() = countCandidateCards() > 1

    private fun countCandidateCards() = candidateCardsIndices.values.fold(0) { acc, e -> acc + e.size }

    private fun updateCandidateCardsIndices(tableDeck: Deck) {
        val topCard = tableDeck.topCard()

        candidateCardsIndices = if (topCard != null) {
            mapOf(
                "same_suite" to playerHand.cards.filter { it.suit == topCard.suit }.map { playerHand.cards.indexOf(it) },
                "same_rank" to playerHand.cards.filter { it.rank == topCard.rank }.map { playerHand.cards.indexOf(it) }
            )
        } else {
            mapOf("same_suite" to emptyList(), "same_rank" to emptyList())
        }
    }
}

class Game {
    private val mainDeck: MainDeck
    private val tableDeck: Deck
    private val players: List<Player>

    private lateinit var lastWinner: Player

    init {
        println("Indigo Card Game")

        mainDeck = MainDeck()
        players = createPlayers()

        tableDeck = mainDeck.popCards(4)
        println("Initial cards on the table: $tableDeck")
    }

    private fun createPlayers(): List<Player> {
        val playFirst: Boolean
        while (true) {
            println("Play first?")

            playFirst = when (readln()) {
                "yes" -> true
                "no" -> false
                else -> continue
            }

            break
        }

        return when (playFirst) {
            true -> listOf(Player(), ComputerPlayer())
            false -> listOf(ComputerPlayer(), Player())
        }
    }

    private fun printInfo() {
        if (tableDeck.size() > 0) {
            println("\n${tableDeck.size()} cards on the table, and the top card is ${tableDeck.topCard()}")
        } else {
            println("\nNo cards on the table")
        }
    }

    private fun scorePlayer(player: Player): Int {
        val onePointers = "A|10|J|Q|K".toRegex()
        return player.wonCards.cards.count { card -> card.rank.matches(onePointers) }
    }

    private fun scorePlayers(player1: Player, player2: Player, endGame: Boolean = false) {
        var score1 = scorePlayer(player1)
        var score2 = scorePlayer(player2)
        val deck1Size = player1.wonCards.size()
        val deck2Size = player2.wonCards.size()

        if (endGame) {
            if (deck1Size > deck2Size) {
                score1 += 3
            } else if (deck1Size < deck2Size) {
                score2 += 3
            } else {
                val firstPlayer = players.first()
                if (player1 == firstPlayer) {
                    score1 += 3
                } else if (player2 == firstPlayer) {
                    score2 += 3
                }
            }
        }

        val playerScore: Int
        val playerCards: Int
        val computerScore: Int
        val computerCards: Int

        if (player2 is ComputerPlayer) {
            playerScore = score1
            playerCards = deck1Size
            computerScore = score2
            computerCards = deck2Size
        } else {
            playerScore = score2
            playerCards = deck2Size
            computerScore = score1
            computerCards = deck1Size
        }

        println(
            "Score: Player $playerScore - Computer $computerScore\n" +
                    "Cards: Player $playerCards - Computer $computerCards"
        )
    }

    private fun checkWin(player: Player): Boolean {
        val playerCategory = if (player is ComputerPlayer) "Computer" else "Player"

        if (tableDeck.topCard()!!.rank == tableDeck.penultimate().rank || tableDeck.topCard()!!.suit == tableDeck.penultimate().suit) {
            println("$playerCategory wins cards")
            lastWinner = player
            return true
        }

        return false
    }

    private fun turn(player: Player) {
        if (!mainDeck.isNotEmpty() && !player.hasCardsInHand()) {
            return
        }

        if (!player.hasCardsInHand()) {
            player.takeCardsToHand(mainDeck.popCards(6))
        }

        player.makeTurn(tableDeck)

        if (tableDeck.size() > 1 && checkWin(player)) {
            player.takeWonCards(tableDeck.popCards(tableDeck.size()))
            scorePlayers(players.first(), players.last())
        }
    }

    fun run() {
        while (true) {
            printInfo()

            if (mainDeck.size() == 0 && players.none { player -> player.hasCardsInHand() }) {
                if (tableDeck.size() > 0) {
                    val remainingCards = tableDeck.popRemainingCards()
                    if (players.none { player -> player.wonAnyCards() }) {
                        players.first().takeWonCards(remainingCards)
                    } else {
                        lastWinner.takeWonCards(remainingCards)
                    }
                }

                scorePlayers(players.first(), players.last(), true)
                throw GameOverException("Game Over")
            }

            turn(players.first())
            printInfo()
            turn(players.last())
        }
    }
}

fun main() {
    val game = Game()

    try {
        game.run()
    } catch (e: GameOverException) {
        println(e.message)
    }
}
