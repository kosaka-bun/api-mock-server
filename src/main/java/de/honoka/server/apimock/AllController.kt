package de.honoka.server.apimock

import cn.hutool.core.exceptions.ExceptionUtil
import cn.hutool.json.JSONConfig
import cn.hutool.json.JSONObject
import cn.hutool.json.JSONUtil
import de.honoka.sdk.util.framework.web.ApiResponse
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.nio.charset.StandardCharsets
import javax.servlet.http.HttpServletResponse

@RestController
class AllController(
    private val mainProperties: MainProperties
) {

    @RequestMapping("/{*path}", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun onRequest(@PathVariable path: String, response: HttpServletResponse): String {
        var apiResponse: ApiResponse<*>
        val file = File("${mainProperties.responseFileBase}$path.json")
        run {
            if(!file.exists()) {
                apiResponse = ApiResponse<Any>().apply {
                    code = HttpStatus.NOT_FOUND.value()
                }
                return@run
            }
            val content = BufferedReader(FileReader(file, StandardCharsets.UTF_8)).use {
                it.readText()
            }
            apiResponse = try {
                JSONUtil.toBean(content, ApiResponse::class.java)
            } catch(t: Throwable) {
                ApiResponse<JSONObject>().apply {
                    code = HttpStatus.INTERNAL_SERVER_ERROR.value()
                    msg = "路径“${path}”下的文件不符合ApiResponse的格式"
                    data = JSONObject().also {
                        it["exception"] = ExceptionUtil.getMessage(t)
                    }
                }
            }
        }
        if(apiResponse.code != null && apiResponse.code != 0) response.status = apiResponse.code
        else response.status = HttpStatus.OK.value()
        return JSONUtil.toJsonStr(apiResponse, JSONConfig().apply {
            isIgnoreNullValue = false
        })
    }
}