/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.operator.common;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.PodTemplateSpec;
import io.fabric8.kubernetes.client.CustomResource;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import static java.lang.Boolean.parseBoolean;
import static java.lang.Integer.parseInt;

/**
 * Class for holding some annotation keys and utility methods for handling annotations
 */
public class Annotations {
    /**
     * The Strimzi domain used in all annotations
     */
    public static final String STRIMZI_DOMAIN = "strimzi.io/";

    /**
     * Strimzi logging annotation
     */
    public static final String STRIMZI_LOGGING_ANNOTATION = STRIMZI_DOMAIN + "logging";

    /**
     * Annotations for rolling a cluster whenever the logging (or it's part) has changed.
     * By changing the annotation we force a restart since the pod will be out of date compared to the statefulset.
     */
    public static final String ANNO_STRIMZI_LOGGING_HASH = STRIMZI_DOMAIN + "logging-hash";

    /**
     * Annotation for tracking changes to logging appenders which cannot be changed dynamically
     */
    public static final String ANNO_STRIMZI_LOGGING_APPENDERS_HASH = STRIMZI_DOMAIN + "logging-appenders-hash";

    /**
     * Annotation for tracking authentication changes
     */
    public static final String ANNO_STRIMZI_AUTH_HASH = STRIMZI_DOMAIN + "auth-hash";

    /**
     * Annotation which enabled the use of the connector operator
     */
    public static final String STRIMZI_IO_USE_CONNECTOR_RESOURCES = STRIMZI_DOMAIN + "use-connector-resources";

    /**
     * Annotation used to store the revision of the Kafka Connect build (hash of the Dockerfile)
     */
    public static final String STRIMZI_IO_CONNECT_BUILD_REVISION = STRIMZI_DOMAIN + "connect-build-revision";

    /**
     * Annotation used to force rebuild of the container image even if the dockerfile did not changed
     */
    public static final String STRIMZI_IO_CONNECT_FORCE_REBUILD = STRIMZI_DOMAIN + "force-rebuild";

    /**
     * Annotation used to pause resource reconciliation
     */
    public static final String ANNO_STRIMZI_IO_PAUSE_RECONCILIATION = STRIMZI_DOMAIN + "pause-reconciliation";

    /**
     * Annotation to trigger manually rolling updats
     */
    public static final String ANNO_STRIMZI_IO_MANUAL_ROLLING_UPDATE = STRIMZI_DOMAIN + "manual-rolling-update";

    /**
     * This annotation with related possible values (approve, stop, refresh) is set by the user for interacting
     * with the rebalance operator in order to start, stop, or refresh rebalancing proposals and operations.
     */
    public static final String ANNO_STRIMZI_IO_REBALANCE = STRIMZI_DOMAIN + "rebalance";

    /**
     * Use this boolean annotation to auto-approve a rebalance optimization proposal without the need for the
     * manual approval by applying the strimzi.io/rebalance=approve annotation
     */
    public static final String ANNO_STRIMZI_IO_REBALANCE_AUTOAPPROVAL = STRIMZI_DOMAIN + "rebalance-auto-approval";

    /**
     * Annotation for restarting KafkaConnector
     */
    public static final String ANNO_STRIMZI_IO_RESTART = STRIMZI_DOMAIN + "restart";

    /**
     * Annotation for restarting Mirror Maker 2 connector
     */
    public static final String ANNO_STRIMZI_IO_RESTART_CONNECTOR = STRIMZI_DOMAIN + "restart-connector";

    /**
     * Annotation for restarting KafkaConnector task
     */
    public static final String ANNO_STRIMZI_IO_RESTART_TASK = STRIMZI_DOMAIN + "restart-task";

    /**
     * Annotation for restarting Mirror Maker 2 connector task
     */
    public static final String ANNO_STRIMZI_IO_RESTART_CONNECTOR_TASK = STRIMZI_DOMAIN + "restart-connector-task";

    /**
     * Key for specifying which Mirror Maker 2 connector should be restarted
     */
    public static final String ANNO_STRIMZI_IO_RESTART_CONNECTOR_TASK_PATTERN_CONNECTOR = "connector";

    /**
     * Key for specifying which Mirror Maker 2 connector task should be restarted
     */
    public static final String ANNO_STRIMZI_IO_RESTART_CONNECTOR_TASK_PATTERN_TASK = "task";

    /**
     * Pattern for validation of value which specifies which connector or task should be restarted
     */
    public static final Pattern ANNO_STRIMZI_IO_RESTART_CONNECTOR_TASK_PATTERN = Pattern.compile("^(?<" +
        ANNO_STRIMZI_IO_RESTART_CONNECTOR_TASK_PATTERN_CONNECTOR +
        ">.+):(?<" +
        ANNO_STRIMZI_IO_RESTART_CONNECTOR_TASK_PATTERN_TASK +
        ">\\d+)$");

    /**
     * Annotation for tracking Deployment revisions
     */
    public static final String ANNO_DEP_KUBE_IO_REVISION = "deployment.kubernetes.io/revision";

    /**
     * List of predicates that allows existing load balancer service annotations to be retained while reconciling the resources.
     */
    public static final List<Predicate<String>> LOADBALANCER_ANNOTATION_IGNORELIST = List.of(
        annotation -> annotation.startsWith("cattle.io/"),
        annotation -> annotation.startsWith("field.cattle.io")
    );

    private static Map<String, String> annotations(ObjectMeta metadata) {
        Map<String, String> annotations = metadata.getAnnotations();
        if (annotations == null) {
            annotations = new HashMap<>(3);
            metadata.setAnnotations(annotations);
        }
        return annotations;
    }

    /**
     * Gets annotations map from a Kubernetes resource
     *
     * @param resource  Resource from which we want to get the annotations
     *
     * @return  Map with annotations
     */
    public static Map<String, String> annotations(HasMetadata resource) {
        return annotations(resource.getMetadata());
    }

    /**
     * Gets annotations from Pod Template in Deployments, StatefulSets and so on
     *
     * @param podSpec  Pod template from which we want to get the annotations
     *
     * @return  Map with annotations
     */
    public static Map<String, String> annotations(PodTemplateSpec podSpec) {
        return annotations(podSpec.getMetadata());
    }

    /**
     * Gets a boolean value of an annotation from a Kubernetes resource
     *
     * @param resource                  Resource from which the annotation should be extracted
     * @param annotation                Annotation key for which we want the value
     * @param defaultValue              Default value if the annotation is not present
     * @param deprecatedAnnotations     Alternative annotations which should be checked if the main annotation is not present
     *
     * @return  Boolean value form the annotation, the fallback annotations or the default value
     */
    public static boolean booleanAnnotation(HasMetadata resource, String annotation, boolean defaultValue, String... deprecatedAnnotations) {
        ObjectMeta metadata = resource.getMetadata();
        String str = annotation(annotation, null, metadata, deprecatedAnnotations);
        return str != null ? parseBoolean(str) : defaultValue;
    }

    /**
     * Gets a boolean value of an annotation from a Kubernetes metadata object
     *
     * @param metadata                  Metadata object from which the annotation should be extracted
     * @param annotation                Annotation key for which we want the value
     * @param defaultValue              Default value if the annotation is not present
     * @param deprecatedAnnotations     Alternative annotations which should be checked if the main annotation is not present
     *
     * @return  Boolean value form the annotation, the fallback annotations or the default value
     */
    private static boolean booleanAnnotation(ObjectMeta metadata, String annotation, boolean defaultValue, String... deprecatedAnnotations) {
        String str = annotation(annotation, null, metadata, deprecatedAnnotations);
        return str != null ? parseBoolean(str) : defaultValue;
    }

    /**
     * Gets an integer value of an annotation from a Kubernetes resource
     *
     * @param resource                  Resource from which the annotation should be extracted
     * @param annotation                Annotation key for which we want the value
     * @param defaultValue              Default value if the annotation is not present
     * @param deprecatedAnnotations     Alternative annotations which should be checked if the main annotation is not present
     *
     * @return  Integer value form the annotation, the fallback annotations or the default value
     */
    public static int intAnnotation(HasMetadata resource, String annotation, int defaultValue, String... deprecatedAnnotations) {
        ObjectMeta metadata = resource.getMetadata();
        String str = annotation(annotation, null, metadata, deprecatedAnnotations);
        return str != null ? parseInt(str) : defaultValue;
    }

    /**
     * Gets a string value of an annotation from a Kubernetes resource
     *
     * @param resource                  Resource from which the annotation should be extracted
     * @param annotation                Annotation key for which we want the value
     * @param defaultValue              Default value if the annotation is not present
     * @param deprecatedAnnotations     Alternative annotations which should be checked if the main annotation is not present
     *
     * @return  String value form the annotation, the fallback annotations or the default value
     */
    public static String stringAnnotation(HasMetadata resource, String annotation, String defaultValue, String... deprecatedAnnotations) {
        ObjectMeta metadata = resource.getMetadata();
        String str = annotation(annotation, null, metadata, deprecatedAnnotations);
        return str != null ? str : defaultValue;
    }

    /**
     * Gets an integer value of an annotation from a Por template
     *
     * @param podSpec                   Por template from which the annotation should be extracted
     * @param annotation                Annotation key for which we want the value
     * @param defaultValue              Default value if the annotation is not present
     * @param deprecatedAnnotations     Alternative annotations which should be checked if the main annotation is not present
     *
     * @return  Integer value form the annotation, the fallback annotations or the default value
     */
    public static int intAnnotation(PodTemplateSpec podSpec, String annotation, int defaultValue, String... deprecatedAnnotations) {
        ObjectMeta metadata = podSpec.getMetadata();
        String str = annotation(annotation, null, metadata, deprecatedAnnotations);
        return str != null ? parseInt(str) : defaultValue;
    }

    /**
     * Gets a string value of an annotation from a Por template
     *
     * @param podSpec                   Por template from which the annotation should be extracted
     * @param annotation                Annotation key for which we want the value
     * @param defaultValue              Default value if the annotation is not present
     * @param deprecatedAnnotations     Alternative annotations which should be checked if the main annotation is not present
     *
     * @return  String value form the annotation, the fallback annotations or the default value
     */
    public static String stringAnnotation(PodTemplateSpec podSpec, String annotation, String defaultValue, String... deprecatedAnnotations) {
        ObjectMeta metadata = podSpec.getMetadata();
        String str = annotation(annotation, null, metadata, deprecatedAnnotations);
        return str != null ? str : defaultValue;
    }

    /**
     * Checks if Kubernetes resource has an annotation with given key
     *
     * @param resource      Kubernetes resource which should be checked for the annotations presence
     * @param annotation    Annotation key
     *
     * @return  True if the annotation exists. False otherwise.
     */
    public static boolean hasAnnotation(HasMetadata resource, String annotation) {
        ObjectMeta metadata = resource.getMetadata();
        String str = annotation(annotation, null, metadata, null);
        return str != null;
    }

    private static String annotation(String annotation, String defaultValue, ObjectMeta metadata, String... deprecatedAnnotations) {
        Map<String, String> annotations = annotations(metadata);
        return annotation(annotation, defaultValue, annotations, deprecatedAnnotations);
    }

    private static String annotation(String annotation, String defaultValue, Map<String, String> annotations, String... deprecatedAnnotations) {
        String value = annotations.get(annotation);
        if (value == null) {
            if (deprecatedAnnotations != null) {
                for (String deprecated : deprecatedAnnotations) {
                    value = annotations.get(deprecated);
                    if (value != null) {
                        break;
                    }
                }
            }

            if (value == null) {
                value = defaultValue;
            }
        }
        return value;
    }

    /**
     * Checks if the custom resource has the paused-reconciliation annotation and returns the value
     *
     * @param resource  Kubernetes resource
     *
     * @return True if the provided resource instance has the strimzi.io/pause-reconciliation annotation and has it set to true. False otherwise.
     */
    public static boolean isReconciliationPausedWithAnnotation(CustomResource resource) {
        return Annotations.booleanAnnotation(resource, ANNO_STRIMZI_IO_PAUSE_RECONCILIATION, false);
    }

    /**
     * Checks if the metadata has the paused-reconciliation annotation and returns the value
     *
     * @param metadata  Kubernetes resource
     *
     * @return True if the metadata instance has the strimzi.io/pause-reconciliation annotation and has it set to true. False otherwise.
     */
    public static boolean isReconciliationPausedWithAnnotation(ObjectMeta metadata) {
        return Annotations.booleanAnnotation(metadata, ANNO_STRIMZI_IO_PAUSE_RECONCILIATION, false);
    }

}
