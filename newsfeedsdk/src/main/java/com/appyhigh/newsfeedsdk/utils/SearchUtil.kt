package com.appyhigh.newsfeedsdk.utils

import io.reactivex.rxjava3.core.Observable
import org.simpleframework.xml.Element
import org.simpleframework.xml.ElementList
import org.simpleframework.xml.Root
import retrofit2.http.GET
import retrofit2.http.Query


interface TrendingSearchesApi {
    @GET("rss")
    fun getTrendingSearches(@Query("geo") countryCode: String): Observable<TrendingSearchResponseWrapper>
}

@Root(name = "rss", strict = false)
class TrendingSearchResponseWrapper @JvmOverloads constructor(
    @field: Element(name = "channel")
    var channel: TrendingSearchResponse? = null
)

@Root(name = "channel", strict = false)
class TrendingSearchResponse @JvmOverloads constructor(
    @field: ElementList(inline = true)
    var itemList: List<TrendingSearchItem>? = null
)

@Root(name = "item", strict = false)
class TrendingSearchItem @JvmOverloads constructor(
    @field: Element(name = "title")
    var title: String = "",
    @field: Element(name = "description", required = false)
    var description: String = "",
    @field: Element(name = "link")
    var link: String = ""
)