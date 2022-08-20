package cinema

const val DEFAULT_PRICE = 10
const val FRONT_PRICE = 10
const val BACK_PRICE = 8

fun buildCinema(): MutableList<MutableList<Char>> {
    println("Enter the number of rows:")
    val rows = readln().toInt()
    println("Enter the number of seats in each row:")
    val seats = readln().toInt()

    return MutableList(rows) { MutableList(seats) { 'S' } }
}

fun renderCinema(cinemaModel: MutableList<MutableList<Char>>) {
    println("\nCinema:")

    // print seats indices
    val seatsIntIndices = (0..cinemaModel[0].size).toList()
    val seatsCharIndices = seatsIntIndices.map { index -> index.toString() }.toMutableList()
    seatsCharIndices[0] = " "
    println(seatsCharIndices.joinToString(separator = " "))

    // print rows
    cinemaModel.forEachIndexed { index, row -> println("${index + 1} ${row.joinToString(separator = " ")}") }
}

fun calculateSeatPrice(cinemaModel: MutableList<MutableList<Char>>, selectedRow: Int): Int {
    val rows = cinemaModel.size
    val seats = cinemaModel[0].size
    val seatsTotal = rows * seats

    val price = if (seatsTotal <= 60) {
        DEFAULT_PRICE
    } else {
        val frontRows = rows / 2
        if (selectedRow <= frontRows) FRONT_PRICE else BACK_PRICE
    }

    println("\nTicket price: $$price")

    return price
}

fun calculateTotalIncome(cinemaModel: MutableList<MutableList<Char>>): Int {
    val rows = cinemaModel.size
    val seats = cinemaModel[0].size
    val seatsTotal = rows * seats

    if (seatsTotal <= 60) {
        return seatsTotal * DEFAULT_PRICE
    } else {
        val frontRows = rows / 2
        val frontSeats = frontRows * seats

        val backRows = rows - frontRows
        val backSeats = backRows * seats

        return frontSeats * FRONT_PRICE + backSeats * BACK_PRICE
    }
}

fun bookSeat(cinemaModel: MutableList<MutableList<Char>>): Int {
    println("\nEnter a row number:")
    val selectedRowIndex = readln().toInt() - 1
    println("Enter a seat number in that row:")
    val selectedSeatIndex = readln().toInt() - 1

    if (selectedRowIndex > cinemaModel.lastIndex || selectedSeatIndex > cinemaModel[0].lastIndex) {
        throw IllegalArgumentException("Wrong input!")
    }

    if (cinemaModel[selectedRowIndex][selectedSeatIndex] == 'B') {
        throw IllegalArgumentException("That ticket has already been purchased!")
    }

    cinemaModel[selectedRowIndex][selectedSeatIndex] = 'B'

    return calculateSeatPrice(cinemaModel, selectedRowIndex + 1)
}

fun main() {
    val cinemaModel: MutableList<MutableList<Char>> = buildCinema()

    var purchasedTickets = 0
    var currentIncome = 0
    val seatsTotal = cinemaModel.size * cinemaModel[0].size
    val totalIncome = calculateTotalIncome(cinemaModel)

    while (true) {
        println("\n1. Show the seats")
        println("2. Buy a ticket")
        println("3. Statistics")
        println("0. Exit")

        when (readln().toInt()) {
            0 -> break
            1 -> renderCinema(cinemaModel)
            2 -> {
                do {
                    var success = false
                    try {
                        val seatPrice = bookSeat(cinemaModel)

                        purchasedTickets++
                        currentIncome += seatPrice
                        success = true
                    } catch (e: java.lang.IllegalArgumentException) {
                        println("\n${e.message}")
                    }
                } while (!success)
            }
            3 -> {
                println("Number of purchased tickets: $purchasedTickets")

                val percentage = purchasedTickets / seatsTotal.toDouble() * 100
                val formatPercentage = "%.2f".format(percentage)
                println("Percentage: $formatPercentage%")

                println("Current income: $$currentIncome")
                println("Total income: $$totalIncome")
            }
        }
    }
}