package utap.tjp2677.antimatter

import java.text.SimpleDateFormat
import java.util.*
import java.io.Serializable

data class Article (
    val title: String,
    val content: String,
    val url: String,
    val published: Date?,
    val image: String?,

    val author: Author,
    val publication: Publication,
) : Serializable

data class Author (
    val name: String,
    val url: String?
) : Serializable

data class Publication (
    val name: String,
    val logo: String?
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
            publication = Publication(name = "Polygon", logo = "https://cdn.vox-cdn.com/community_logos/51927/38.png"),
            content = """
                <figure> <img alt="Silent Hill 3 - Cheryl Mason shines her flashlight into the camera. She is a young woman with messy blonde hair in a white vest and orange top. She looks disturbed and on edge." src="https://cdn.vox-cdn.com/thumbor/t4s70eTkCFddGPP0vIbOf2evgwQ=/53x0:1376x744/640x360/cdn.vox-cdn.com/uploads/chorus_image/image/71546724/TtG2F5K.0.png" /> <figcaption>Image: Konami</figcaption> </figure> <p>Time for Princess Heart to shine</p> <p> <a href="https://www.polygon.com/23424874/dead-by-daylight-leaks-silent-hill-heather-cheryl-princess-heart-cosmetic">Continue reading&hellip;</a> </p>
            """.trimIndent(),
            image = "https://cdn.vox-cdn.com/thumbor/t4s70eTkCFddGPP0vIbOf2evgwQ=/53x0:1376x744/640x360/cdn.vox-cdn.com/uploads/chorus_image/image/71546724/TtG2F5K.0.png"
        )

        private val testArticle2 = Article(
            title = "Spain temporarily closed its airspace due to an out-of-control Chinese rocket",
            url = "https://www.engadget.com/long-march-5b-uncontrolled-rentry-november-184505247.html?src=rss",
            published = formatter.parse("2022-11-06 18:45:22"),
            author = Author(name = "Igor Bonifacic", url = "https://www.engadget.com/about/editors/igor-bonifacic/"),
            publication = Publication(name = "Engadget", logo = "https://s.yimg.com/kw/assets/favicon-160x160.png"),
            content = """
                <p>For the second time this year, the uncontrolled remnants of a Chinese Long March 5B came crashing to Earth. On Friday morning, US Space Command <a href="https://twitter.com/US_SpaceCom/status/1588502881631887361?s=20&amp;t=DfsgYf5IcAjaIYXw4lWWJQ"><ins>confirmed</ins></a> pieces of the rocket that carried the third and final piece of China's Tiangong space station to orbit had re-entered the planet’s atmosphere over the south-central Pacific Ocean, reports <a href="https://www.nytimes.com/2022/11/04/science/china-rocket-debris.html"><em><ins>The New York Times</ins></em></a>. The debris eventually plunged into the body of water, leaving no one harmed.</p><p>The episode marked the fourth uncontrolled re-entry for China’s most powerful heavy-lift rocket following its debut in 2020. Unlike many of its modern counterparts, including the SpaceX Falcon 9, the Long March 5B can’t reignite its engine to complete a predictable descent back to Earth. The rocket has yet to harm anyone (and probably won’t in the future). Still, each time China has sent a Long March 5B into space, astronomers and onlookers have anxiously followed its path back to the surface, worrying it might land somewhere people live. On Friday, Spain <a href="https://www.bbc.com/news/world-europe-63513070"><ins>briefly closed parts of its airspace</ins></a> over risks posed by the debris from Monday’s mission, leading to hundreds of flight delays.</p><span id="end-legacy-contents"></span><p>As he did earlier this year <a href="https://www.engadget.com/china-long-march-5b-re-entry-182425022.html"><ins>following China’s Wentian mission</ins></a>, NASA Administrator Bill Nelson criticized the country for not taking the appropriate precautions to prevent an out-of-control re-entry. “It is critical that all spacefaring nations are responsible and transparent in their space activities, and follow established best practices, especially, for the uncontrolled re-entry of a large rocket body debris — debris that could very well result in major damage or loss of life,” he said.</p><p>Space debris landing on Earth isn’t a problem unique to China. In August, for instance, a farmer in rural Australia found a piece of a <a href="https://www.nytimes.com/2022/08/04/world/australia/spacex-debris-australia.html"><ins>SpaceX Crew Dragon</ins></a> spacecraft that landed on his farm. However, many experts stress that those incidents differ from the one that occured on Friday. “The thing I want to point out about this is that we, the world, don’t deliberately launch things this big intending them to fall wherever,” Ted Muelhaupt, an <a href="https://aerospacecorp.medium.com/a-massive-chinese-rocket-is-falling-uncontrollably-to-earth-db7c7b32d773"><ins>Aerospace Corporation</ins></a> consultant, told <em>The Times</em>. “We haven’t done that for 50 years.” China will launch another Long March 5B rocket next year when it attempts to put its Xuntian space telescope into orbit.</p>
            """.trimIndent(),
            image = "https://s.yimg.com/os/creatr-uploaded-images/2022-11/4f563a30-5e02-11ed-aedb-bfc05ca09924"
        )

        private val n_articles = 7
        private var initialDataList = MutableList(n_articles) { testArticle }
    }

    init {
        initialDataList.add(testArticle2)
    }

    fun fetchData(): List<Article> {
        return initialDataList
    }

}