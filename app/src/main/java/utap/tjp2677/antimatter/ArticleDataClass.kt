package utap.tjp2677.antimatter

import java.text.SimpleDateFormat
import java.util.*
import java.io.Serializable

data class Article (
    val title: String,
    val content: String,
    val url: String,
    val published: Date?,

    val author: Author,
    val publication: Publication,
) : Serializable

data class Author (
    val name: String,
    val url: String?
) : Serializable

data class Publication (
    val name: String,
) : Serializable

class Repository {
    companion object {
        private val timezone = TimeZone.getTimeZone("America/New_York")
        private val formatter = SimpleDateFormat("dd-mm-yyyy hh:mm:ss", Locale.ENGLISH)

        private val testArticle = Article(
            title = "Dead by Daylight leaks reveal Cheryl’s magical girl outfit is on the way",
            url = "https://www.polygon.com/23424874/dead-by-daylight-leaks-silent-hill-heather-cheryl-princess-heart-cosmetic",
            published = formatter.parse("2022-10-26 14:19:50"),
            author = Author(name = "Cass Marshall", url = null),
            publication = Publication(name = "Polygon - All"),
            content = """
                        <figure> <img alt="Silent Hill 3 - Cheryl Mason shines her flashlight into the camera. She is a young woman with messy blonde hair in a white vest and orange top. She looks disturbed and on edge." src="https://cdn.vox-cdn.com/thumbor/t4s70eTkCFddGPP0vIbOf2evgwQ=/53x0:1376x744/640x360/cdn.vox-cdn.com/uploads/chorus_image/image/71546724/TtG2F5K.0.png" /> <figcaption>Image: Konami</figcaption> </figure> <p>Time for Princess Heart to shine</p> <p> <a href="https://www.polygon.com/23424874/dead-by-daylight-leaks-silent-hill-heather-cheryl-princess-heart-cosmetic">Continue reading&hellip;</a> </p>
                    """.trimIndent()
        )
        private val n_articles = 10

        private var initialDataList = List(n_articles) { testArticle }
    }

    fun fetchData(): List<Article> {
        return initialDataList
    }
}