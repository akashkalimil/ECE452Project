package com.teamred.candid.vision;

import java.util.List;

import javax.annotation.Nullable;

public class BatchResponse {

    public class Response {
        @Nullable
        public List<CloudVision.FaceAnnotation> faceAnnotations;

        public boolean hasFaces() {
            return faceAnnotations != null;
        }
    }

    List<Response> responses;
}
