package de.honoka.server.apimock.component

import cn.hutool.json.JSONObject
import cn.hutool.json.JSONUtil
import de.honoka.server.apimock.data.ResponseFileData
import de.honoka.server.apimock.util.CustomJsonUtils
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
        var file = Path.of(mainProperties.responseFileBase!!, "$path.json5").toFile()
        if(!file.exists()) file = Path.of(mainProperties.responseFileBase!!, "$path.plain.json5").toFile()
        if(!file.exists()) throw FileNotFoundException("the response files of $path are all not found")
        val isPlain = file.name.endsWith("plain.json5", true)
        //读取文件
        val content = file.reader().use { CustomJsonUtils.json5ToJson(it.readText()) }
        //生成响应
        if(isPlain) {
            //判断并修改HTTP状态码
            response.status = guessHttpStatus(JSONUtil.parse(content))
            //响应
            return content
        }
        //处理复杂数据的响应
        val fileData = JSONUtil.toBean(content, ResponseFileData::class.java)
        val resInfo = fileData.responses!![fileData.active ?: 0]
        response.status = resInfo?.httpStatus ?: guessHttpStatus(resInfo?.body)
        resInfo?.headers?.run {
            keys.forEach {
                response.addHeader(it, get(it)?.toString() ?: "")
            }
        }
        return resInfo?.body!!.toString()
    }

    private fun guessHttpStatus(obj: Any?): Int {
        return if(obj is JSONObject) {
            obj.getInt("code")?.let {
                when(it) {
                    0, HttpStatus.OK.value() -> HttpStatus.OK.value()
                    else -> HttpStatus.INTERNAL_SERVER_ERROR.value()
                }
            } ?: HttpStatus.OK.value()
        } else {
            HttpStatus.OK.value()
        }
    }
}