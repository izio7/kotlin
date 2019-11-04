/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.core.script.configuration.cache

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.containers.SLRUMap
import org.jetbrains.kotlin.scripting.resolve.ScriptCompilationConfigurationWrapper

class ScriptConfigurationMemoryCache(
    val project: Project
) : ScriptConfigurationCache {
    companion object {
        const val MAX_SCRIPTS_CACHED = 50
    }

    private val memoryCache = SLRUMap<VirtualFile, ScriptConfigurationState>(MAX_SCRIPTS_CACHED, MAX_SCRIPTS_CACHED)

    @Synchronized
    override operator fun get(file: VirtualFile): ScriptConfigurationState? {
        return memoryCache.get(file)
    }

    @Synchronized
    override fun setApplied(file: VirtualFile, configurationSnapshot: ScriptConfigurationSnapshot) {
        val old = memoryCache[file]
        if (old != null) {
            memoryCache.put(file, old.copy(applied = configurationSnapshot))
        } else {
            memoryCache.put(file, ScriptConfigurationState(configurationSnapshot))
        }
    }

    @Synchronized
    override fun setLoaded(file: VirtualFile, configurationSnapshot: ScriptConfigurationSnapshot) {
        val old = memoryCache[file]
        if (old != null) {
            memoryCache.put(file, old.copy(applied = configurationSnapshot))
        } else {
            memoryCache.put(file, ScriptConfigurationState(configurationSnapshot, configurationSnapshot))
        }
    }

    @Synchronized
    override fun markOutOfDate(file: VirtualFile) {
        val old = memoryCache[file]
        if (old != null) {
            memoryCache.put(
                file, old.copy(
                    applied = old.applied.copy(inputs = CachedConfigurationInputs.OutOfDate),
                    loaded = old.loaded?.copy(inputs = CachedConfigurationInputs.OutOfDate)
                )
            )
        }
    }

    @Synchronized
    @Suppress("UNCHECKED_CAST")
    override fun allApplied() =
        memoryCache.entrySet().map {
            if (it.value.applied.configuration == null) null
            else it.key to it.value.applied.configuration
        } as Collection<Pair<VirtualFile, ScriptCompilationConfigurationWrapper>>
}