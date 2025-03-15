/*
 * (C) 2025 GAHOJIN, Inc.
 */
package jp.co.gahojin.refreshVersions.dependency

import jp.co.gahojin.refreshVersions.Version
import java.io.StringReader
import javax.xml.stream.XMLInputFactory
import javax.xml.stream.XMLStreamReader

/**
 * メタデータXMLパーサー.
 */
object MavenMetadataParser {
    fun parse(xml: String): List<Version> {
        val factory: XMLInputFactory = XMLInputFactory.newInstance()
        // 外部DTDを読み込まない
        factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false)
        factory.setProperty(XMLInputFactory.SUPPORT_DTD, false)

        var reader: XMLStreamReader? = null
        var isInTag = false
        val buffer = StringBuilder()
        val result = mutableListOf<Version>()
        try {
            reader = factory.createXMLStreamReader(StringReader(xml))
            while (reader.hasNext()) {
                when (reader.next()) {
                    XMLStreamReader.START_ELEMENT -> {
                        if (reader.name.localPart == "version") {
                            buffer.clear()
                            isInTag = true
                        }
                    }
                    XMLStreamReader.CHARACTERS -> {
                        if (isInTag) {
                            buffer.append(reader.text)
                        }
                    }
                    XMLStreamReader.END_ELEMENT -> {
                        if (isInTag && reader.name.localPart == "version") {
                            isInTag = false
                            result.add(Version(buffer.toString()))
                        }
                    }
                }
            }
        } finally {
            reader?.close()
        }
        return result
    }
}
