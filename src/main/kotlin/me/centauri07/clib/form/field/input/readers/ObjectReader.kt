package me.centauri07.clib.form.field.input.readers

import me.centauri07.clib.form.field.input.InputReader
import java.awt.Color
import java.net.HttpURLConnection
import java.net.URL
import java.util.*

class StringReader: InputReader<String>(String::class.java) {
    override val converter: (String) -> String = String::toString
}

class ColorReader: InputReader<Color>(Color::class.java) {
    override val converter: (String) -> Color? = {
        Arrays.stream(clazz.declaredFields).filter {
            declaredField -> declaredField.type.isAssignableFrom(clazz) && declaredField.name.equals(it.replace(" ", ""), true)
        }.findFirst().orElse(null)?.get(clazz) as Color
    }
}

class URLReader: InputReader<URL>(URL::class.java) {
    override val converter: (String) -> URL? = {
        if ((URL(it).openConnection() as HttpURLConnection).responseCode == HttpURLConnection.HTTP_OK) URL(it)

        null
    }
}