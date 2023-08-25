package de.honoka.server.apimock

data class ResponseFileData(

    var active: Int? = null,

    var responses: List<ResponseDetailsData?>? = null
)

data class ResponseDetailsData(

    var httpStatus: Int? = null,

    var headers: Map<String, Any?>? = null,

    var body: Any? = null
)