package com.teamred.candid.vision;

import java.util.List;

import javax.annotation.Nullable;

public class BatchResponse {

    public class Response {
        @Nullable
        public List<CloudVision.FaceAnnotation> annotations;

        public boolean hasFaces() {
            return annotations != null;
        }
    }

    List<Response> responses;
}
