package com.dartmoor.tors

data class Tor(
    val id: Int,
    val name: String,
    val heightMeters: Int,
    val heightFeet: Int,
    val gridReference: String,
    val latitude: Double,
    val longitude: Double,
    val description: String
)

object TorRepository {
    fun getAllTors(): List<Tor> = listOf(
        Tor(
            id = 1,
            name = "High Willhays",
            heightMeters = 621,
            heightFeet = 2037,
            gridReference = "SX 580 892",
            latitude = 50.6917,
            longitude = -4.0167,
            description = "High Willhays is the highest point on Dartmoor and in southern England. Located on the northern part of the moor, it offers stunning views across Devon."
        ),
        Tor(
            id = 2,
            name = "Yes Tor",
            heightMeters = 619,
            heightFeet = 2030,
            gridReference = "SX 581 901",
            latitude = 50.7006,
            longitude = -4.0158,
            description = "Yes Tor is the second highest point on Dartmoor, located near High Willhays. It features impressive granite outcrops and is a popular hiking destination."
        ),
        Tor(
            id = 3,
            name = "Hay Tor",
            heightMeters = 457,
            heightFeet = 1499,
            gridReference = "SX 757 770",
            latitude = 50.5769,
            longitude = -3.7550,
            description = "Hay Tor is one of the most iconic and visited tors on Dartmoor, featuring impressive granite rock formations. It offers panoramic views and is easily accessible."
        ),
        Tor(
            id = 4,
            name = "Belstone Tor",
            heightMeters = 529,
            heightFeet = 1736,
            gridReference = "SX 619 915",
            latitude = 50.7144,
            longitude = -3.9658,
            description = "Belstone Tor sits on the northern edge of Dartmoor, providing spectacular views across the surrounding countryside and towards Okehampton."
        ),
        Tor(
            id = 5,
            name = "Vixen Tor",
            heightMeters = 320,
            heightFeet = 1050,
            gridReference = "SX 541 745",
            latitude = 50.5525,
            longitude = -4.0661,
            description = "Vixen Tor is one of the most striking rock formations on Dartmoor, with a distinctive profile. It has been the subject of access disputes but remains a notable landmark."
        ),
        Tor(
            id = 6,
            name = "Hound Tor",
            heightMeters = 448,
            heightFeet = 1470,
            gridReference = "SX 742 789",
            latitude = 50.5947,
            longitude = -3.7731,
            description = "Hound Tor is famous for its medieval village ruins nearby and its impressive rock formations. It's a popular spot with fascinating historical connections."
        ),
        Tor(
            id = 7,
            name = "Bowerman's Nose",
            heightMeters = 404,
            heightFeet = 1325,
            gridReference = "SX 741 805",
            latitude = 50.6089,
            longitude = -3.7742,
            description = "Bowerman's Nose is a distinctive rock stack that stands like a sentinel on the hillside. Legend says it's a huntsman turned to stone."
        ),
        Tor(
            id = 8,
            name = "Great Mis Tor",
            heightMeters = 539,
            heightFeet = 1768,
            gridReference = "SX 562 771",
            latitude = 50.5772,
            longitude = -4.0400,
            description = "Great Mis Tor is a prominent tor in the western part of Dartmoor, offering excellent views. It features substantial granite outcrops."
        ),
        Tor(
            id = 9,
            name = "Great Staple Tor",
            heightMeters = 450,
            heightFeet = 1476,
            gridReference = "SX 541 760",
            latitude = 50.5656,
            longitude = -4.0664,
            description = "Great Staple Tor is located near Merrivale, featuring impressive granite formations and good views of the surrounding moorland."
        ),
        Tor(
            id = 10,
            name = "Rippon Tor",
            heightMeters = 473,
            heightFeet = 1552,
            gridReference = "SX 748 751",
            latitude = 50.5611,
            longitude = -3.7678,
            description = "Rippon Tor offers fine views across eastern Dartmoor and features interesting rock formations at its summit."
        )
    )
    
    fun getTorById(id: Int): Tor? = getAllTors().find { it.id == id }
}
