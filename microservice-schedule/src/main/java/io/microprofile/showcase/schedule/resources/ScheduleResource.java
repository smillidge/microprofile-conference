/*
 * Copyright 2016 Microprofile.io
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
package io.microprofile.showcase.schedule.resources;

import io.microprofile.showcase.schedule.model.Schedule;
import io.microprofile.showcase.schedule.persistence.ScheduleDAO;
import io.swagger.annotations.Api;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

import javax.cache.annotation.*;

@Path("/")
@Api(description = "Schedule REST Endpoint")
@RequestScoped
@Produces("application/json")
@CacheDefaults(cacheName = "java-one")
public class ScheduleResource {

    @Inject
    private ScheduleDAO scheduleDAO;

    @PUT
    @Consumes("application/json")
    @CachePut
    public Schedule add(@QueryParam("id") @CacheKey Long id, @CacheValue Schedule schedule) {
        Schedule created = scheduleDAO.addSchedule(schedule);
        return created;
//        return Response.created(URI.create("/" + created.getId()))
//                        .entity(created)
//                        .build();
    }

    @GET
    @Path("/{id}")
    @CacheResult
    public Schedule retrieve(@PathParam("id") @CacheKey Long id) {
        return scheduleDAO.findById(id).get();
//            .map(schedule -> Response.ok(schedule).build())
//            .orElse(Response.status(Response.Status.NOT_FOUND).build());
    }
    

    @GET
    @Path("/all")
    public Response allSchedules() {
        List<Schedule> allSchedules = scheduleDAO.getAllSchedules();
        GenericEntity<List<Schedule>> entity = buildEntity(allSchedules);
        return Response.ok(entity).build();
    }

    @GET
    @Path("/venue/{venue}")
    public Response allForVenue(@PathParam("venue") String venue) {
        List<Schedule> schedulesByVenue = scheduleDAO.findByVenue(venue);
        GenericEntity<List<Schedule>> entity = buildEntity(schedulesByVenue);
        return Response.ok(entity).build();
    }

    @GET
    @Path("/active/{dateTime}")
    public Response activeAtDate(@PathParam("dateTime") String dateTimeString) {
        LocalDateTime dateTime = LocalDateTime.parse(dateTimeString);
        List<Schedule> schedulesByDate = scheduleDAO.findByDate(dateTime.toLocalDate());
        List<Schedule> activeAtTime = schedulesByDate.stream()
            .filter(schedule -> isTimeInSchedule(dateTime.toLocalTime(), schedule))
            .collect(Collectors.toList());
        GenericEntity<List<Schedule>> entity = buildEntity(activeAtTime);
        return Response.ok(entity).build();
    }

    @GET
    @Path("/all/{date}")
    public Response allForDay(@PathParam("date") String dateString) {
        LocalDate date = LocalDate.parse(dateString);
        List<Schedule> schedulesByDate = scheduleDAO.findByDate(date);
        GenericEntity<List<Schedule>> entity = buildEntity(schedulesByDate);
        return Response.ok(entity).build();
    }

    @DELETE
    @Path("/{scheduleId}")
    public Response remove(@PathParam("scheduleId") Long scheduleId) {
        scheduleDAO.deleteSchedule(scheduleId);
        return Response.noContent().build();
    }

    private GenericEntity<List<Schedule>> buildEntity(final List<Schedule> scheduleList) {
        return new GenericEntity<List<Schedule>>(scheduleList) {};
    }

    private boolean isTimeInSchedule(LocalTime currentTime, Schedule schedule) {
        LocalTime scheduleStartTime = schedule.getStartTime();
        LocalTime scheduleEndTime = scheduleStartTime.plus(schedule.getDuration());
        return scheduleStartTime.isBefore(currentTime) &&
            scheduleEndTime.isAfter(currentTime);
    }
}
