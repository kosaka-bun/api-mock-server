package de.honoka.server.apimock

import cn.hutool.json.JSONObject
import cn.hutool.json.JSONUtil
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.io.FileNotFoundException
import java.nio.file.Path
import javax.servlet.http.HttpServletResponse

@CrossOrigin
@RestController
class AllController(
    private val mainProperties: MainProperties
) {

    @RequestMapping("/{*path}", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun onRequest(@PathVariable path: String, response: HttpServletResponse): String {
        //寻找文件
        var file = Path.of(mainProperties.responseFileBase!!, "$path.json").toFile()
        if(!file.exists()) file = Path.of(mainProperties.responseFileBase!!, "$path.plain.json").toFile()
        if(!file.exists()) throw FileNotFoundException("the response files of $path are all not found")
        val isPlain = file.name.endsWith("plain.json", true)
        //读取文件
        val content = file.reader().use { it.readText() }
        val json = JSONUtil.parse(content)
        //生成响应
        if(isPlain) {
            //判断并修改HTTP状态码
            val status = if(json is JSONObject) {
                val code = json.getInt("code")
                if(code == null || code == 0) {
                    HttpStatus.OK.value()
                } else {
                    HttpStatus.INTERNAL_SERVER_ERROR.value()
                }
            } else HttpStatus.OK.value()
            response.status = status
            //响应
            return content
        }
        //处理复杂数据的响应
        val jo = json as JSONObject
        val resInfo = jo.getJSONArray("responses").getJSONObject(jo.getInt("active") ?: 0).also {
            it ?: throw IndexOutOfBoundsException("the active index exceeded the max index")
        }
        response.status = resInfo.getInt("http_status") ?: HttpStatus.OK.value()
        resInfo.getJSONObject("headers")?.run {
            keys.forEach {
                response.addHeader(it, getStr(it) ?: "")
            }
        }
        return resInfo["body"]?.toString() ?: throw NullPointerException("response body is null")
    }
}