package com.example.a0328kotlin_recy

import android.os.Parcel
import android.os.Parcelable

data class MainData (
    val name: String,
    val add: Int,
    val play: Int
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString()!!,
        parcel.readInt(),
        parcel.readInt()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(name)
        parcel.writeInt(add)
        parcel.writeInt(play)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<MainData> {
        override fun createFromParcel(parcel: Parcel): MainData {
            return MainData(parcel)
        }

        override fun newArray(size: Int): Array<MainData?> {
            return arrayOfNulls(size)
        }
    }
}
