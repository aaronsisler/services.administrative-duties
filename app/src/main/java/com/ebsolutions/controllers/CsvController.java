package com.ebsolutions.controllers;

import com.ebsolutions.exceptions.DataProcessingException;
import com.ebsolutions.models.CsvResponse;
import com.ebsolutions.services.OrchestrationService;
import com.ebsolutions.utils.UniqueIdGenerator;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Post;

import static io.micronaut.http.HttpResponse.accepted;
import static io.micronaut.http.HttpResponse.serverError;

@Controller("/csv")
public class CsvController {
    private final OrchestrationService orchestrationService;

    public CsvController(OrchestrationService orchestrationService) {
        this.orchestrationService = orchestrationService;
    }

    @Post
    public HttpResponse<?> postCsv() {
        try {
            String trackingId = UniqueIdGenerator.generate();
            this.orchestrationService.createCsv(trackingId);

            return accepted().body(CsvResponse.builder().trackingId(trackingId).build());
        } catch (DataProcessingException dbe) {
            return serverError(dbe);
        }
    }
}