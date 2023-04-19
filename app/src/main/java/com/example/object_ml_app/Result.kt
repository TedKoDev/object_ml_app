package com.example.a0413yolov8kotlin

import android.graphics.RectF
import java.io.File
import java.io.FileNotFoundException


// 모델에서 추론한 결과를 저장하는 클래스  classIndex = 검출 객체의 번호, score = 검출 객체의 확률, rectF = 검출 객체의 위치
data class Result(val classIndex: Int, val score: Float, val rectF: RectF)