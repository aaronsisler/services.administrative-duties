package com.ebsolutions.dal.daos;

import com.ebsolutions.config.DatabaseConstants;
import com.ebsolutions.dal.dtos.WorkshopDto;
import com.ebsolutions.dal.utils.KeyBuilder;
import com.ebsolutions.exceptions.DataProcessingException;
import com.ebsolutions.models.MetricsStopWatch;
import com.ebsolutions.models.Workshop;
import com.ebsolutions.utils.UniqueIdGenerator;
import io.micronaut.context.annotation.Prototype;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;

import java.text.MessageFormat;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import static software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional.sortBeginsWith;

@Slf4j
@Prototype
public class WorkshopDao {
    private DynamoDbTable<WorkshopDto> ddbTable;

    public WorkshopDao(DynamoDbEnhancedClient enhancedClient) {
        this.ddbTable = enhancedClient.table(DatabaseConstants.DATABASE_TABLE_NAME, TableSchema.fromBean(WorkshopDto.class));
    }

    public Workshop read(String clientId, String workshopId) {
        MetricsStopWatch metricsStopWatch = new MetricsStopWatch();
        try {
            Key key = KeyBuilder.build(clientId, DatabaseConstants.WORKSHOP_SORT_KEY, workshopId);

            WorkshopDto workshopDto = ddbTable.getItem(key);

            return workshopDto == null
                    ? null
                    : Workshop.builder()
                    .clientId(workshopDto.getPartitionKey())
                    .workshopId(StringUtils.remove(workshopDto.getSortKey(), DatabaseConstants.WORKSHOP_SORT_KEY))
                    .locationId(StringUtils.remove(workshopDto.getLocationId(), DatabaseConstants.LOCATION_SORT_KEY))
                    .organizerId(StringUtils.remove(workshopDto.getOrganizerId(), DatabaseConstants.ORGANIZER_SORT_KEY))
                    .name(workshopDto.getName())
                    .category(workshopDto.getCategory())
                    .description(workshopDto.getDescription())
                    .workshopDate(workshopDto.getWorkshopDate())
                    .startTime(workshopDto.getStartTime())
                    .duration(workshopDto.getDuration())
                    .createdOn(workshopDto.getCreatedOn())
                    .lastUpdatedOn(workshopDto.getLastUpdatedOn())
                    .build();
        } catch (DynamoDbException dbe) {
            log.error("ERROR::{}", this.getClass().getName(), dbe);
            throw new DataProcessingException("Error in {}".formatted(this.getClass().getName()), dbe);
        } catch (Exception e) {
            log.error("ERROR::{}", this.getClass().getName(), e);
            throw new DataProcessingException("Error in {}".formatted(this.getClass().getName()), e);
        } finally {
            metricsStopWatch.logElapsedTime(MessageFormat.format("{0}::{1}", this.getClass().getName(), "read"));
        }
    }

    public List<Workshop> readAll(String clientId) {
        MetricsStopWatch metricsStopWatch = new MetricsStopWatch();
        try {
            List<WorkshopDto> workshopDtos = ddbTable
                    .query(r -> r.queryConditional(
                            sortBeginsWith(s
                                    -> s.partitionValue(clientId).sortValue(DatabaseConstants.WORKSHOP_SORT_KEY).build()))
                    )
                    .items()
                    .stream()
                    .collect(Collectors.toList());

            return workshopDtos.stream()
                    .map(workshopDto ->
                            Workshop.builder()
                                    .clientId(workshopDto.getPartitionKey())
                                    .workshopId(StringUtils.remove(workshopDto.getSortKey(), DatabaseConstants.WORKSHOP_SORT_KEY))
                                    .locationId(StringUtils.remove(workshopDto.getLocationId(), DatabaseConstants.LOCATION_SORT_KEY))
                                    .organizerId(StringUtils.remove(workshopDto.getOrganizerId(), DatabaseConstants.ORGANIZER_SORT_KEY))
                                    .name(workshopDto.getName())
                                    .category(workshopDto.getCategory())
                                    .description(workshopDto.getDescription())
                                    .workshopDate(workshopDto.getWorkshopDate())
                                    .startTime(workshopDto.getStartTime())
                                    .duration(workshopDto.getDuration())
                                    .createdOn(workshopDto.getCreatedOn())
                                    .lastUpdatedOn(workshopDto.getLastUpdatedOn())
                                    .build()
                    ).collect(Collectors.toList());

        } catch (DynamoDbException dbe) {
            log.error("ERROR::{}", this.getClass().getName(), dbe);
            throw new DataProcessingException("Error in {}".formatted(this.getClass().getName()), dbe);
        } catch (Exception e) {
            log.error("ERROR::{}", this.getClass().getName(), e);
            throw new DataProcessingException("Error in {}".formatted(this.getClass().getName()), e);
        } finally {
            metricsStopWatch.logElapsedTime(MessageFormat.format("{0}::{1}", this.getClass().getName(), "read"));
        }
    }

    public void delete(String clientId, String workshopId) {
        MetricsStopWatch metricsStopWatch = new MetricsStopWatch();
        try {
            Key key = KeyBuilder.build(clientId, DatabaseConstants.WORKSHOP_SORT_KEY, workshopId);

            ddbTable.deleteItem(key);

        } catch (DynamoDbException dbe) {
            log.error("ERROR::{}", this.getClass().getName(), dbe);
            throw new DataProcessingException("Error in {}".formatted(this.getClass().getName()), dbe);
        } catch (Exception e) {
            log.error("ERROR::{}", this.getClass().getName(), e);
            throw new DataProcessingException("Error in {}".formatted(this.getClass().getName()), e);
        } finally {
            metricsStopWatch.logElapsedTime(MessageFormat.format("{0}::{1}", this.getClass().getName(), "read"));
        }
    }

    public Workshop create(Workshop workshop) {
        MetricsStopWatch metricsStopWatch = new MetricsStopWatch();
        try {
            LocalDateTime now = LocalDateTime.now();
            WorkshopDto workshopDto = WorkshopDto.builder()
                    .partitionKey(workshop.getClientId())
                    .sortKey(DatabaseConstants.WORKSHOP_SORT_KEY + UniqueIdGenerator.generate())
                    .locationId(DatabaseConstants.LOCATION_SORT_KEY + workshop.getLocationId())
                    .organizerId(DatabaseConstants.ORGANIZER_SORT_KEY + workshop.getOrganizerId())
                    .name(workshop.getName())
                    .category(workshop.getCategory())
                    .description(workshop.getDescription())
                    .workshopDate(workshop.getWorkshopDate())
                    .startTime(workshop.getStartTime())
                    .duration(workshop.getDuration())
                    .createdOn(now)
                    .lastUpdatedOn(now)
                    .build();

            ddbTable.updateItem(workshopDto);

            return Workshop.builder()
                    .clientId(workshopDto.getPartitionKey())
                    .workshopId(StringUtils.remove(workshopDto.getSortKey(), DatabaseConstants.WORKSHOP_SORT_KEY))
                    .locationId(StringUtils.remove(workshopDto.getLocationId(), DatabaseConstants.LOCATION_SORT_KEY))
                    .organizerId(StringUtils.remove(workshopDto.getOrganizerId(), DatabaseConstants.ORGANIZER_SORT_KEY))
                    .name(workshopDto.getName())
                    .category(workshopDto.getCategory())
                    .description(workshopDto.getDescription())
                    .workshopDate(workshopDto.getWorkshopDate())
                    .startTime(workshopDto.getStartTime())
                    .duration(workshopDto.getDuration())
                    .createdOn(workshopDto.getCreatedOn())
                    .lastUpdatedOn(workshopDto.getLastUpdatedOn())
                    .build();
        } catch (DynamoDbException dbe) {
            log.error("ERROR::{}", this.getClass().getName(), dbe);
            throw new DataProcessingException("Error in {}".formatted(this.getClass().getName()), dbe);
        } catch (Exception e) {
            log.error("ERROR::{}", this.getClass().getName(), e);
            throw new DataProcessingException("Error in {}".formatted(this.getClass().getName()), e);
        } finally {
            metricsStopWatch.logElapsedTime(MessageFormat.format("{0}::{1}", this.getClass().getName(), "read"));
        }
    }

    /**
     * This will replace the entire database object with the input client
     *
     * @param workshop the object to replace the current database object
     */
    public void update(Workshop workshop) {
        MetricsStopWatch metricsStopWatch = new MetricsStopWatch();
        try {
            WorkshopDto workshopDto = WorkshopDto.builder()
                    .partitionKey(workshop.getClientId())
                    .sortKey(DatabaseConstants.WORKSHOP_SORT_KEY + workshop.getWorkshopId())
                    .locationId(DatabaseConstants.LOCATION_SORT_KEY + workshop.getLocationId())
                    .organizerId(DatabaseConstants.ORGANIZER_SORT_KEY + workshop.getOrganizerId())
                    .name(workshop.getName())
                    .category(workshop.getCategory())
                    .description(workshop.getDescription())
                    .workshopDate(workshop.getWorkshopDate())
                    .startTime(workshop.getStartTime())
                    .duration(workshop.getDuration())
                    .createdOn(workshop.getCreatedOn())
                    .lastUpdatedOn(LocalDateTime.now())
                    .build();

            ddbTable.putItem(workshopDto);

        } catch (DynamoDbException dbe) {
            log.error("ERROR::{}", this.getClass().getName(), dbe);
            throw new DataProcessingException("Error in {}".formatted(this.getClass().getName()), dbe);
        } catch (Exception e) {
            log.error("ERROR::{}", this.getClass().getName(), e);
            throw new DataProcessingException("Error in {}".formatted(this.getClass().getName()), e);
        } finally {
            metricsStopWatch.logElapsedTime(MessageFormat.format("{0}::{1}", this.getClass().getName(), "read"));
        }
    }
}
