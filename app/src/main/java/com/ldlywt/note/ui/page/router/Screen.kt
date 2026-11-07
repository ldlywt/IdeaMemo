package com.ldlywt.note.ui.page.router

import kotlinx.serialization.Serializable


@Serializable
sealed class Screen {

    @Serializable
    data object Main : Screen()

    @Serializable
    data object Welcome : Screen()

    @Serializable
    data object Explore : Screen()

    @Serializable
    data class InputDetail(val id: Long) : Screen()

    @Serializable
    data class MemoPreview(val id: Long) : Screen()

    @Serializable
    object TagList : Screen()

    @Serializable
    data class TagDetail(val tag: String) : Screen()

    @Serializable
    data class YearDetail(val year: String) : Screen()

    @Serializable
    data class DateRangePage(val startTime: Long, val endTime: Long) : Screen()

    @Serializable
    data class LocationDetail(val location: String) : Screen()

    @Serializable
    object Search : Screen()

    @Serializable
    data class Share(val id: Long) : Screen()

    @Serializable
    object DataManager : Screen()

    @Serializable
    object Statistics : Screen()

    @Serializable
    object RandomWalk : Screen()

    @Serializable
    object Gallery : Screen()

    @Serializable
    data class PictureDisplay(val pathList: List<String>, val curIndex: Int, val timestamps: List<Long>) : Screen()

    @Serializable
    object MoreInfo : Screen()

    @Serializable
    object DonatePage : Screen()

    @Serializable
    data class CommentList(val parentNoteId: Long) : Screen()

    @Serializable
    object RecycleBin : Screen()
}