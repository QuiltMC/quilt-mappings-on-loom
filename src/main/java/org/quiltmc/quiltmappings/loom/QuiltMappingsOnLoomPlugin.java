package org.quiltmc.quiltmappings.loom;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

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
        target.getExtensions().create("quiltMappings", QuiltMappingsOnLoomExtension.class);

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

        @Deprecated
        public MappingsSpec<?> mappings(String quiltMappings, boolean snapshot) {
            return mappings(quiltMappings);
        }

        public MappingsSpec<?> mappings(String quiltMappings) {
            return new MappingLayerMappingsSpec(project, quiltMappings);
        }

        private record MappingLayerMappingsSpec(Project project, String quiltMappings)
                implements MappingsSpec<MappingLayer> {
            @Override
            public MappingLayer createLayer(MappingContext context) {
                return new QuiltMappingsLayer(context, project, quiltMappings);
            }

            @Override
            public int hashCode() {
                return Objects.hash(quiltMappings);
            }
        }
    }

    private record QuiltMappingsLayer(MappingContext context, Project project, String quiltMappings)
            implements MappingLayer {
        @Override
        public void visit(MappingVisitor mappingVisitor) throws IOException {
            String minecraftVersion = context.minecraftProvider().minecraftVersion();

            String quiltMappingsBuild = "+build" + quiltMappings.substring(quiltMappings.lastIndexOf("."), quiltMappings.lastIndexOf(":"));
            File intermediaryToQm = project.file(".gradle/qm/qm_to_intermediary_" + minecraftVersion + quiltMappingsBuild + ".tiny");

            if (!intermediaryToQm.exists()) {
                List<File> quiltMappings = new ArrayList<>(project.getConfigurations().detachedConfiguration(project.getDependencies().create(this.quiltMappings)).resolve());

                File hashedFile = project.file(".gradle/qm/hashed_" + minecraftVersion + quiltMappingsBuild + ".tiny");
                downloadFile(quiltMappings.get(1), hashedFile);

                File quiltMappingsFile = project.file(".gradle/qm/qm_" + minecraftVersion + quiltMappingsBuild + ".tiny");
                downloadFile(quiltMappings.get(0), quiltMappingsFile);

                MemoryMappingTree mappings = new MemoryMappingTree();

                MemoryMappingTree hashed = new MemoryMappingTree();
                try (FileReader reader = new FileReader(hashedFile)) {
                    Tiny2Reader.read(reader, hashed);
                }
                hashed.accept(mappings);

                try (FileReader reader = new FileReader(quiltMappingsFile)) {
                    Tiny2Reader.read(reader, mappings);
                }

                try (MappingWriter writer = MappingWriter.create(new FileWriter(intermediaryToQm), MappingFormat.TINY_2)) {
                    mappings.accept(writer);
                }
            }

            Tiny2Reader.read(new FileReader(intermediaryToQm), mappingVisitor);
        }

        private void downloadFile(File dependency, File output) {
            if (!output.exists()) {
                GFileUtils.copyFile(project
                        .zipTree(dependency)
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
    }
}
