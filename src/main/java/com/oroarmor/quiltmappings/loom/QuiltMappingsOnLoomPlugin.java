package com.oroarmor.quiltmappings.loom;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.util.GFileUtils;

import net.fabricmc.loom.api.mappings.layered.MappingContext;
import net.fabricmc.loom.api.mappings.layered.MappingLayer;
import net.fabricmc.loom.api.mappings.layered.MappingsNamespace;
import net.fabricmc.loom.api.mappings.layered.spec.MappingsSpec;
import net.fabricmc.mappingio.MappingVisitor;
import net.fabricmc.mappingio.MappingWriter;
import net.fabricmc.mappingio.adapter.MappingDstNsReorder;
import net.fabricmc.mappingio.adapter.MappingNsRenamer;
import net.fabricmc.mappingio.adapter.MappingSourceNsSwitch;
import net.fabricmc.mappingio.format.MappingFormat;
import net.fabricmc.mappingio.format.Tiny2Reader;
import net.fabricmc.mappingio.tree.MemoryMappingTree;

@SuppressWarnings("UnstableApiUsage")
public class QuiltMappingsOnLoomPlugin implements Plugin<Project> {
    @Override
    public void apply(Project target) {
        target.getExtensions().create("quiltmappings", QuiltMappingsOnLoomExtension.class, target);

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

        public MappingsSpec<?> mappings(String quiltMappings, boolean snapshot) {
            return new MappingsSpec<>() {
                @Override
                public MappingLayer createLayer(MappingContext context) {
                    return new QuiltMappingsLayer(context, project, quiltMappings, snapshot);
                }

                @Override
                public int hashCode() {
                    return Objects.hash(quiltMappings, snapshot);
                }
            };
        }
    }

    private record QuiltMappingsLayer(MappingContext context, Project project, String quiltMappings,
                                      boolean snapshot) implements MappingLayer {

        @Override
        public void visit(MappingVisitor mappingVisitor) throws IOException {
            String minecraftVersion = context.minecraftProvider().minecraftVersion();

            File intermediaryToQm = project.file(".gradle/qm/qm_to_intermediary_" + quiltMappings + ".tiny");

            if (!intermediaryToQm.exists()) {
                Set<File> quiltmappings = project.getConfigurations().detachedConfiguration(project.getDependencies().create(quiltMappings)).resolve();
                Set<File> hashedFiles = project.getConfigurations().detachedConfiguration(project.getDependencies().create("org.quiltmc:hashed:" + minecraftVersion + (snapshot ? "-SNAPSHOT" : ""))).resolve();

                File hashedFile = project.file(".gradle/qm/hashed_" + minecraftVersion + ".tiny");
                downloadFile(hashedFiles, hashedFile);

                File quiltMappingsFile = project.file(".gradle/qm/qm_" + minecraftVersion + ".tiny");
                downloadFile(quiltmappings, quiltMappingsFile);

                MemoryMappingTree mappings = new MemoryMappingTree();
                MappingSourceNsSwitch sourceNsSwitch = new MappingSourceNsSwitch(mappings, MappingsNamespace.OFFICIAL.toString());

                MemoryMappingTree hashed = new MemoryMappingTree();
                try (FileReader reader = new FileReader(hashedFile)) {
                    Tiny2Reader.read(reader, hashed);
                }
                hashed.accept(sourceNsSwitch);

                try (FileReader reader = new FileReader(quiltMappingsFile)) {
                    Tiny2Reader.read(reader, mappings);
                }

                try (MappingWriter writer = MappingWriter.create(new FileWriter(intermediaryToQm), MappingFormat.TINY_2)) {
                    mappings.accept(writer);
                }
            }

            Tiny2Reader.read(new FileReader(intermediaryToQm), mappingVisitor);
        }

        private void downloadFile(Set<File> dependency, File output) {
            if (!output.exists()) {
                GFileUtils.copyFile(project
                        .zipTree(dependency.stream().iterator().next())
                        .getFiles()
                        .stream()
                        .filter(file -> file.getName().endsWith("mappings.tiny"))
                        .findFirst()
                        .get(), output);
            }
        }

        @Override
        public MappingsNamespace getSourceNamespace() {
            return MappingsNamespace.OFFICIAL;
        }

        @Override
        public int hashCode() {
            return quiltMappings.hashCode();
        }
    }
}
