package com.bluetrainsoftware.maven

import org.apache.maven.project.MavenProject
import org.apache.maven.artifact.Artifact
import groovy.xml.MarkupBuilder
import org.apache.maven.plugin.MojoFailureException
import org.apache.maven.artifact.metadata.ResolutionGroup
import org.apache.maven.artifact.repository.ArtifactRepository
import org.apache.maven.artifact.metadata.ArtifactMetadataRetrievalException
import org.apache.maven.artifact.metadata.ArtifactMetadataSource

class ReleaseTemplate {
  MavenProject project
  Set<Artifact> artifacts
  ArtifactMetadataSource artifactMetadataSource
  ArtifactRepository localRepository

  public ReleaseTemplate(MavenProject project, Set<Artifact> artifacts,
                         ArtifactMetadataSource artifactMetadataSource,
                         ArtifactRepository localRepository) {
    this.project = project
    this.artifacts = artifacts
    this.artifactMetadataSource = artifactMetadataSource
    this.localRepository = localRepository
  }

  List<String> ignoreScopes = ["test", "provided", "optional"]

  List<Artifact> getExcludes(Artifact artifact) throws MojoFailureException {
    List<Artifact> excludes = []

    ResolutionGroup resolutionGroup;

    try {
      resolutionGroup = artifactMetadataSource.retrieve(artifact, localRepository, [artifact.repository?:localRepository]);
    } catch (ArtifactMetadataRetrievalException e) {
      throw new MojoFailureException("Cannot retrieve " + artifact.toString(), e);
    }

    for (Artifact dep : resolutionGroup.getArtifacts()) {
      if (!dep.isOptional() && !ignoreScopes.contains(dep.getScope())) // these won't make it in anyway
        excludes.add(dep)
    }

    return excludes
  }


  public String generateReleasePom() throws MojoFailureException {
    StringWriter writer = new StringWriter()
    def xml = new MarkupBuilder(writer)
    xml.project() {
      if (project.parent) {
        parent() {
          groupId(project.parent.groupId)
          artifactId(project.parent.artifactId)
          version(project.parent.version)
        }
      }

      if (!project.parent || project.parent.groupId != project.groupId)
        groupId(project.groupId)

      artifactId(project.artifactId)
      version(project.version)
      name(project.name)
      description(project.description)

      properties() {
        project.properties.each { key, value ->
          "${key}"(value)
        }
      }

      if (artifacts) {
        dependencies() {
          artifacts.each { Artifact artifact ->
//            dependency() {
//              groupId(artifact.groupId)
//              artifactId(artifact.artifactId)
//              version("[" + artifact.getVersion() + "]")
//              def excludedArtifacts = getExcludes(artifact)
//              if (excludes) {
//                excludes() {
//                  excludedArtifacts.each { Artifact ex ->
//                    exclude() {
//                      groupId(ex.groupId)
//                      artifactId(ex.artifactId)
//                    }
//                  }
//                }
//              }
//            }
          }
        }
      }
    }

    xml.omitEmptyAttributes = true
    return writer.toString()
  }
}