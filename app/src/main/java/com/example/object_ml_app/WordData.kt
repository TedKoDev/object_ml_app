package com.example.a0328kotlin_recy

import android.os.Parcel
import android.os.Parcelable

data class WordData (
    val name: String,
    val update : Int,

    val play: Int
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString()!!,
        parcel.readInt(),
        parcel.readInt()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(name)
        parcel.writeInt(play)
        parcel.writeInt(update)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<WordData> {
        override fun createFromParcel(parcel: Parcel): WordData {
            return WordData(parcel)
        }

        override fun newArray(size: Int): Array<WordData?> {
            return arrayOfNulls(size)
        }
    }
}
