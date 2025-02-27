package com.marioban2dam.cronoaparcamientos

data class Parking(
    var latitude: String? = null,
    var longitude: String? = null,
    var location: String? = null,
    var description: String? = null,
    var url:String? = null
) {
    // No-argument constructor required for Firebase
    constructor() : this(null, null, null, null,null)
}