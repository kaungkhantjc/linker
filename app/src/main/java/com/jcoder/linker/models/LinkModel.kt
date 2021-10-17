package com.jcoder.linker.models

import com.jcoder.linker.data.Link

data class LinkModel(val isLocal: Boolean, val link: Link) {
    override fun equals(other: Any?): Boolean {
        return if (other is LinkModel) {
            if (other.isLocal) {
                other.link.url == link.url
            } else {
                other.link.id == link.id
            }
        } else false
    }

    override fun hashCode(): Int {
        var result = isLocal.hashCode()
        result = 31 * result + link.hashCode()
        return result
    }

    override fun toString(): String {
        return link.url
    }
}