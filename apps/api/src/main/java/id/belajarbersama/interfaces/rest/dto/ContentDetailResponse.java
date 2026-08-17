package id.belajarbersama.interfaces.rest.dto;

import id.belajarbersama.domain.content.ContentBody;
import id.belajarbersama.domain.content.ContentReview;
import id.belajarbersama.domain.content.ContentRevision;
import id.belajarbersama.domain.content.ContentSubmission;
import id.belajarbersama.domain.content.EducationalContent;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ContentDetailResponse(
        UUID id,
        String kind,
        String slug,
        UUID makerId,
        String makerDisplayName,
        UUID subjectId,
        String subjectName,
        UUID educationLevelId,
        String educationLevelName,
        UUID parentId,
        String status,
        boolean publiclyVisible,
        UUID currentRevisionId,
        UUID publishedRevisionId,
        int currentRevisionNumber,
        Instant createdAt,
        Instant updatedAt,
        ContentRevisionResponse currentRevision,
        List<ContentReviewResponse> reviews,
        int sortOrder,
        boolean required,
        List<ChildResponse> children,
        CreateContentRequest.QuizRequest quiz) {
    public record ChildResponse(
            UUID id,
            String slug,
            String kind,
            String title,
            int sortOrder,
            boolean required,
            List<ChildResponse> children) {}

    public record ContentRevisionResponse(
            UUID id,
            int revisionNumber,
            String title,
            String summary,
            ContentBody body,
            String license,
            String changeSummary,
            UUID createdBy,
            Instant createdAt,
            List<UUID> competencyIds,
            List<SourceResponse> sources) {}

    public record SourceResponse(
            UUID id,
            String title,
            String author,
            String publisher,
            String url,
            String publicationInfo,
            String notes) {}

    public record ContentReviewResponse(
            UUID id,
            UUID submissionId,
            UUID revisionId,
            UUID reviewerId,
            String decision,
            String comment,
            Instant createdAt,
            Instant decidedAt) {}

    public record SubmissionResponse(
            UUID id,
            UUID contentId,
            UUID revisionId,
            UUID makerId,
            String status,
            UUID assignedCheckerId,
            Instant createdAt,
            String title) {}

    public static ContentRevisionResponse revision(ContentRevision revision) {
        return new ContentRevisionResponse(
                revision.id(),
                revision.revisionNumber(),
                revision.title(),
                revision.summary(),
                revision.body(),
                revision.license().name(),
                revision.changeSummary(),
                revision.createdBy().value(),
                revision.createdAt(),
                revision.competencyIds(),
                revision.sources().stream()
                        .map(
                                source ->
                                        new SourceResponse(
                                                source.id(),
                                                source.title(),
                                                source.author(),
                                                source.publisher(),
                                                source.url(),
                                                source.publicationInfo(),
                                                source.notes()))
                        .toList());
    }

    public static ContentReviewResponse review(ContentReview review) {
        return new ContentReviewResponse(
                review.id(),
                review.submissionId(),
                review.revisionId(),
                review.reviewerId().value(),
                review.decision() == null ? null : review.decision().name(),
                review.comment(),
                review.createdAt(),
                review.decidedAt());
    }

    public static SubmissionResponse submission(ContentSubmission submission) {
        return submission(submission, null);
    }

    public static SubmissionResponse submission(ContentSubmission submission, String title) {
        return new SubmissionResponse(
                submission.id(),
                submission.contentId(),
                submission.revisionId(),
                submission.makerId().value(),
                submission.status().name(),
                submission.assignedCheckerId() == null
                        ? null
                        : submission.assignedCheckerId().value(),
                submission.createdAt(),
                title);
    }

    public static ContentDetailResponse of(
            EducationalContent content,
            ContentRevision revision,
            String makerName,
            String subjectName,
            String levelName,
            List<ContentReview> reviewList) {
        return new ContentDetailResponse(
                content.id(),
                content.kind().name(),
                content.slug(),
                content.makerId().value(),
                makerName,
                content.subjectId(),
                subjectName,
                content.educationLevelId(),
                levelName,
                content.parentId(),
                content.status().name(),
                content.publiclyVisible(),
                content.currentRevisionId(),
                content.publishedRevisionId(),
                revision.revisionNumber(),
                content.createdAt(),
                content.updatedAt(),
                revision(revision),
                reviewList.stream().map(ContentDetailResponse::review).toList(),
                content.sortOrder(),
                content.required(),
                List.of(),
                null);
    }

    public static ContentDetailResponse of(
            EducationalContent content,
            ContentRevision revision,
            String makerName,
            String subjectName,
            String levelName,
            List<ContentReview> reviewList,
            CreateContentRequest.QuizRequest quiz) {
        return new ContentDetailResponse(
                content.id(),
                content.kind().name(),
                content.slug(),
                content.makerId().value(),
                makerName,
                content.subjectId(),
                subjectName,
                content.educationLevelId(),
                levelName,
                content.parentId(),
                content.status().name(),
                content.publiclyVisible(),
                content.currentRevisionId(),
                content.publishedRevisionId(),
                revision.revisionNumber(),
                content.createdAt(),
                content.updatedAt(),
                revision(revision),
                reviewList.stream().map(ContentDetailResponse::review).toList(),
                content.sortOrder(),
                content.required(),
                List.of(),
                quiz);
    }

    public static ContentDetailResponse publicOf(
            EducationalContent content,
            ContentRevision revision,
            String makerName,
            String subjectName,
            String levelName) {
        return publicOf(content, revision, makerName, subjectName, levelName, List.of());
    }

    public static ContentDetailResponse publicOf(
            EducationalContent content,
            ContentRevision revision,
            String makerName,
            String subjectName,
            String levelName,
            List<ChildResponse> children) {
        return new ContentDetailResponse(
                content.id(),
                content.kind().name(),
                content.slug(),
                content.makerId().value(),
                makerName,
                content.subjectId(),
                subjectName,
                content.educationLevelId(),
                levelName,
                content.parentId(),
                "PUBLISHED",
                true,
                content.publishedRevisionId(),
                content.publishedRevisionId(),
                revision.revisionNumber(),
                content.createdAt(),
                content.updatedAt(),
                revision(revision),
                List.of(),
                content.sortOrder(),
                content.required(),
                children,
                null);
    }
}
