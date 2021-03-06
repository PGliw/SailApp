package com.pwr.sailapp.data.sail

import com.google.gson.annotations.SerializedName


data class Centre(@SerializedName("centre_id")
                  val ID:Int,
                  @SerializedName("centre_name")
                  val name:String,

                  @SerializedName("latitude")
                  val coordinateX:Double,

                  @SerializedName("longitude")
                  val coordinateY:Double,

                  @SerializedName("phone_number")
                  var phone:String,

                  @SerializedName("photo_url")
                  var photoURL:String? = null,

                  var distance:Double = Double.POSITIVE_INFINITY)
{
    override fun toString(): String {
        return "$ID $name $coordinateX $coordinateY"
    }
}
