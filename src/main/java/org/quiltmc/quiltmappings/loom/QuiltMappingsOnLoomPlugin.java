/*
 * Copyright 2021, 2022 QuiltMC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.quiltmc.quiltmappings.loom;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

import net.fabricmc.loom.configuration.providers.mappings.extras.unpick.UnpickLayer;
import net.fabricmc.mappingio.adapter.MappingSourceNsSwitch;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.util.GFileUtils;

import net.fabricmc.loom.api.mappings.layered.MappingContext;
import net.fabricmc.loom.api.mappings.layered.MappingLayer;
import net.fabricmc.loom.api.mappings.layered.MappingsNamespace;
import net.fabricmc.loom.api.mappings.layered.spec.MappingsSpec;
import net.fabricmc.mappingio.MappingVisitor;
import net.fabricmc.mappingio.MappingWriter;
import net.fabricmc.mappingio.format.MappingFormat;
import net.fabricmc.mappingio.format.Tiny2Reader;
import net.fabricmc.mappingio.tree.MemoryMappingTree;

@SuppressWarnings("UnstableApiUsage")
public class QuiltMappingsOnLoomPlugin implements Plugin<Project> {
    @Override
    public void apply(Project target) {
        target.getExtensions().create("quiltMappings", QuiltMappingsOnLoomExtension.class, target);

        target.getRepositories().maven(repo -> {
            repo.setName("Quilt Releases");
            repo.setUrl("https://maven.quiltmc.org/repository/release");
        });

        target.getRepositories().maven(repo -> {
            repo.setName("Quilt Snapshots");
            repo.setUrl("https://maven.quiltmc.org/repository/snapshot");
        });
    }

    public static class QuiltMappingsOnLoomExtension {
        private final Project project;

        public QuiltMappingsOnLoomExtension(Project project) {
            this.project = project;
        }

        @Deprecated(forRemoval = true)
        public MappingsSpec<?> mappings(String quiltMappings, boolean unused) {
            project.getLogger().warn("mappings(String, boolean) is deprecated, please use mappings(String)");
            return mappings(quiltMappings);
        }

        public MappingsSpec<?> mappings(String quiltMappings) {
            return new MappingLayerMappingsSpec(project, quiltMappings);
        }

        private record MappingLayerMappingsSpec(Project project, String quiltMappings)
                implements MappingsSpec<MappingLayer> {
            private static final int HASH_CODE_VERSION_BASE = 1;
            private static final int HASH_CODE_VERSION_EXTRA_BITS = 8;
            private static final int HASH_CODE_VERSION = (1 << HASH_CODE_VERSION_EXTRA_BITS) + HASH_CODE_VERSION_BASE;

            @Override
            public MappingLayer createLayer(MappingContext context) {
                return new QuiltMappingsLayer(context, project, quiltMappings);
            }

            @Override
            public int hashCode() {
                return Objects.hash(quiltMappings, HASH_CODE_VERSION);
            }
        }
    }

    private record QuiltMappingsLayer(MappingContext context, Project project, String quiltMappings)
            implements MappingLayer, UnpickLayer {
        @Override
        public void visit(MappingVisitor mappingVisitor) throws IOException {
            String minecraftVersion = context.minecraftProvider().minecraftVersion();

            String quiltMappingsBuild = "+build" + quiltMappings.substring(quiltMappings.lastIndexOf("."), quiltMappings.lastIndexOf(":"));
            File intermediaryToQm = project.file(".gradle/qm/qm_to_intermediary_" + minecraftVersion + quiltMappingsBuild + ".tiny");

            if (!intermediaryToQm.exists()) {
                List<File> quiltMappings = new ArrayList<>(project.getConfigurations().detachedConfiguration(project.getDependencies().create(this.quiltMappings)).resolve());

                File hashedFile = project.file(".gradle/qm/hashed_" + minecraftVersion + quiltMappingsBuild + ".tiny");
                extractMappings(quiltMappings.get(1), hashedFile);

                File quiltMappingsFile = project.file(".gradle/qm/qm_" + minecraftVersion + quiltMappingsBuild + ".tiny");
                extractMappings(quiltMappings.get(0), quiltMappingsFile);

                MemoryMappingTree mappings = new MemoryMappingTree();

                // Load qm before hashed to avoid losing mappings without hashed names (i.e. unobfuscated names and <init>s)
                try (FileReader reader = new FileReader(quiltMappingsFile)) {
                    Tiny2Reader.read(reader, mappings);
                }

                try (FileReader reader = new FileReader(hashedFile)) {
                    // Change source namespace to hashed to allow merging with qm mapping tree
                    Tiny2Reader.read(reader, new MappingSourceNsSwitch(mappings, "hashed"));
                }

                try (MappingWriter writer = MappingWriter.create(new FileWriter(intermediaryToQm), MappingFormat.TINY_2)) {
                    mappings.accept(new MappingSourceNsSwitch(writer, MappingsNamespace.OFFICIAL.toString()));
                }
            }

            Tiny2Reader.read(new FileReader(intermediaryToQm), mappingVisitor);
        }

        private void extractMappings(File dependency, File output) {
            extractFile(dependency, output, file -> file.getName().endsWith("mappings.tiny"));
        }

        private void extractFile(File dependency, File output, Predicate<File> filter) {
            if (!output.exists()) {
                GFileUtils.copyFile(project
                        .zipTree(dependency)
                        .getFiles()
                        .stream()
                        .filter(filter)
                        .findFirst()
                        .get(), output);
            }
        }

        @Override
        public MappingsNamespace getSourceNamespace() {
            return MappingsNamespace.OFFICIAL;
        }

        @Override
        public UnpickData getUnpickData() throws IOException {
            String minecraftVersion = context.minecraftProvider().minecraftVersion();

            String quiltMappingsBuild = "+build" + quiltMappings.substring(quiltMappings.lastIndexOf("."), quiltMappings.lastIndexOf(":"));
            File definitions = project.file(".gradle/qm/unpick_definitions_" + minecraftVersion + quiltMappingsBuild + ".unpick");
            File metadata = project.file(".gradle/qm/unpick_metadata_" + minecraftVersion + quiltMappingsBuild + ".json");

            if (!definitions.exists() || !metadata.exists()) {
                List<File> quiltMappings = new ArrayList<>(project.getConfigurations().detachedConfiguration(project.getDependencies().create(this.quiltMappings)).resolve());

                extractFile(quiltMappings.get(0), definitions, file -> file.getName().endsWith("definitions.unpick"));
                extractFile(quiltMappings.get(0), metadata, file -> file.getName().endsWith("unpick.json"));
            }

            return UnpickData.read(metadata.toPath(), definitions.toPath());
        }
    }
}
