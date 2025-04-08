package dev.shandeep.EventsDashboardPlugin;

import sailpoint.api.SailPointContext;
import sailpoint.object.Attributes;
import sailpoint.object.Filter;
import sailpoint.object.Identity;
import sailpoint.object.IdentityRequest;
import sailpoint.object.IdentityRequest.CompletionStatus;
import sailpoint.object.QueryOptions;
import sailpoint.object.TaskResult;
import sailpoint.rest.plugin.BasePluginResource;
import sailpoint.rest.plugin.RequiredRight;

import sailpoint.tools.Util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.xml.bind.DatatypeConverter;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

// TO-DO
// Failed Task Results - Events without Identity Request

@RequiredRight("EventsDashboardPluginAccess")
@Path("EventsDashboardPlugin")
public class EventsDashboardPluginRest extends BasePluginResource {
    private static Log log = LogFactory.getLog("dev.shandeep.EDP.EventsDashboardPluginRest");

    @Override
    public String getPluginName() {
        return "EventsDashboardPlugin";
    }

    @RequiredRight("EventsDashboardPluginAccess")
    @GET
    @Path("getUniqueEvents")
    @Produces({ "application/json" })
    public Map<String, List<String>> getUniqueEvents() throws Exception {

        Map<String, List<String>> returnMap = new HashMap<String, List<String>>();
        Set<String> uniqueIdentityRequestTypes = new TreeSet<String>();

        String selectedEvents = getSettingString("selectedEvents");
        if (Util.isNotNullOrEmpty(selectedEvents)) {
            String[] selectedEventsArray = selectedEvents.split(",");
            for (String e : selectedEventsArray) {
                uniqueIdentityRequestTypes.add(e.trim());
            }
            returnMap.put("Events", (List<String>) new ArrayList<String>(uniqueIdentityRequestTypes));
            return returnMap;
        }
        SailPointContext context = getContext();
        QueryOptions identityRequestQueryOptions = new QueryOptions();
        identityRequestQueryOptions.setDistinct(true);
        identityRequestQueryOptions.addFilter(Filter.notnull("type"));
        identityRequestQueryOptions.add(
                Filter.or(Filter.eq("targetClass", "Identity"), Filter.eq("targetClass", "sailpoint.object.Identity")));

        Iterator<Object[]> identityRequestIterator = context.search(IdentityRequest.class, identityRequestQueryOptions,
                "type");

        while (identityRequestIterator.hasNext()) {
            Object[] objArray = (Object[]) identityRequestIterator.next();
            if (objArray.length > 0 && objArray[0] != null) {
                uniqueIdentityRequestTypes.add(objArray[0].toString());
            }
        }

        returnMap.put("Events", (List<String>) new ArrayList<String>(uniqueIdentityRequestTypes));
        return returnMap;
    }

    @RequiredRight("EventsDashboardPluginAccess")
    @GET
    @Path("getAllEvents")
    @Produces({ "application/json" })
    public Map<String, List<Map<String, String>>> getAllEvents(@QueryParam("event") String event,
            @DefaultValue("100") @QueryParam("limit") int limit)
            throws Exception {
        SailPointContext context = getContext();
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy 'at' hh:mm:ss a z");
        Map<String, List<Map<String, String>>> returnMap = new HashMap<String, List<Map<String, String>>>();
        log.debug("Received event parameter: " + (event != null ? event : "no event specified"));
        List<String> eventsToProcess = new ArrayList<String>();
        if (Util.isNotNullOrEmpty(event)) {
            eventsToProcess.add(event);
        } else {
            eventsToProcess.addAll(getUniqueEvents().get("Events"));
        }

        for (String eventName : eventsToProcess) {
            List<Map<String, String>> events = new ArrayList<Map<String, String>>();

            QueryOptions identityRequestQueryOptions = new QueryOptions();
            identityRequestQueryOptions.add(Filter.eq("type", eventName));
            identityRequestQueryOptions.add(
                    Filter.or(Filter.eq("targetClass", "Identity"),
                            Filter.eq("targetClass", "sailpoint.object.Identity")));
            identityRequestQueryOptions.setResultLimit(limit);
            identityRequestQueryOptions.addOrdering("created", false);
            // type - eventName
            // targetId - Identity ID
            // name - IdentityRequest ID
            // created - created in unix time
            // completionStatus - IdentityRequest Status

            Iterator<Object[]> identityRequestIterator = context.search(IdentityRequest.class,
                    identityRequestQueryOptions,
                    "type, targetId, name, created, completionStatus, endDate, requesterDisplayName, requesterId");
            while (identityRequestIterator.hasNext()) {
                Object[] objArray = (Object[]) identityRequestIterator.next();
                if (objArray.length > 0) {

                    String type = (String) objArray[0];
                    String targetID = (String) objArray[1];
                    String identityRequestId = (String) objArray[2];
                    Date createdDate = (Date) objArray[3];
                    CompletionStatus identityRequestCompletionStatus = (CompletionStatus) objArray[4];
                    Date endDate = (Date) objArray[5];
                    String requesterDisplayName = (String) objArray[6];
                    String requesterId = (String) objArray[7];
                    String identityRequestCompletionStatusString = "";
                    if (identityRequestCompletionStatus != null) {
                        identityRequestCompletionStatusString = identityRequestCompletionStatus.toString();
                    } else {
                        identityRequestCompletionStatusString = "Pending";
                    }

                    String taskResultId = "";
                    String taskResultName = "";
                    String taskResultStatus = "";

                    String targetName = "";
                    String targetDisplayName = "";

                    IdentityRequest identityRequest = context.getObjectByName(IdentityRequest.class, identityRequestId);
                    Attributes<String, Object> identityRequestAttributes = identityRequest.getAttributes();
                    if (identityRequestAttributes.containsKey("taskResultId")) {
                        taskResultId = identityRequestAttributes.getString("taskResultId");
                    }
                    context.decache(identityRequest);

                    if (Util.isNotNullOrEmpty(taskResultId)) {
                        TaskResult eventTaskResult = context.getObjectById(TaskResult.class, taskResultId);
                        if (eventTaskResult != null) {
                            taskResultName = eventTaskResult.getName();
                            if (eventTaskResult.getCompletionStatus() != null) {
                                taskResultStatus = eventTaskResult.getCompletionStatus().toString();
                            } else {
                                taskResultStatus = "Running";
                            }
                            context.decache(eventTaskResult);
                        }
                    }

                    if (Util.isNotNullOrEmpty(targetID)) {
                        Identity targetIdentity = context.getObjectById(Identity.class, targetID);
                        if (targetIdentity != null) {
                            targetName = targetIdentity.getName();
                            targetDisplayName = targetIdentity.getDisplayableName();
                            context.decache(targetIdentity);
                        }
                    }

                    String dataToHash = type + targetID + String.valueOf(createdDate.getTime())
                            + (endDate != null ? String.valueOf(endDate.getTime()) : "");
                    String uniqueHash = "";
                    try {
                        MessageDigest digest = MessageDigest.getInstance("SHA-1");
                        byte[] hash = digest.digest(dataToHash.getBytes());
                        uniqueHash = DatatypeConverter.printHexBinary(hash);
                    } catch (NoSuchAlgorithmException e) {
                        log.error("Error generating SHA-1 hash: " + e.getMessage());
                        e.printStackTrace();
                    }

                    Map<String, String> oneEvent = new HashMap<String, String>();
                    oneEvent.put("type", type);
                    oneEvent.put("targetName", targetName);
                    oneEvent.put("targetDisplayName", targetDisplayName);
                    oneEvent.put("targetId", targetID);
                    oneEvent.put("requesterDisplayName", requesterDisplayName);
                    oneEvent.put("requesterId", requesterId);
                    oneEvent.put("identityRequestId", identityRequestId);
                    oneEvent.put("createdDate", sdf.format(createdDate));
                    oneEvent.put("endDate", (endDate != null ? sdf.format(endDate) : ""));
                    oneEvent.put("uniqueHash", uniqueHash);
                    oneEvent.put("taskResultId", taskResultId);
                    oneEvent.put("taskResultName", taskResultName);
                    oneEvent.put("taskResultStatus", taskResultStatus);
                    oneEvent.put("identityRequestCompletionStatusString", identityRequestCompletionStatusString);

                    events.add(oneEvent);
                }
            }
            returnMap.put(eventName, events);
        }
        return returnMap;
    }

    @RequiredRight("EventsDashboardPluginAccess")
    @GET
    @Path("getEventWithFilter")
    @Produces({ "application/json" })
    public Map<String, List<Map<String, String>>> getEventWithFilter(@QueryParam("event") String event,
            @QueryParam("limit") int limit, @QueryParam("page") int page,
            @QueryParam("startDate") String startDate, @QueryParam("endDate") String endDate)
            throws Exception {
        log.debug("Staring getEventWithFilter Method");
        log.debug("event - " + event);
        log.debug("limit - " + limit);
        log.debug("page - " + page);
        log.debug("startDate - " + startDate);
        log.debug("endDate - " + endDate);
        SailPointContext context = getContext();
        SimpleDateFormat jsDateFormat = new SimpleDateFormat("yyyy-MM-dd");
        Date startDateObj = jsDateFormat.parse(startDate);
        Date endDateObj = jsDateFormat.parse(endDate);

        log.debug("startDateObj - " + startDateObj);
        log.debug("endDateObj - " + endDateObj);

        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy 'at' hh:mm:ss a z");
        Map<String, List<Map<String, String>>> returnMap = new HashMap<String, List<Map<String, String>>>();

        List<Map<String, String>> events = new ArrayList<Map<String, String>>();

        QueryOptions identityRequestQueryOptions = new QueryOptions();
        identityRequestQueryOptions.add(Filter.eq("type", event));
        identityRequestQueryOptions.add(
                Filter.or(Filter.eq("targetClass", "Identity"),
                        Filter.eq("targetClass", "sailpoint.object.Identity")));
        identityRequestQueryOptions.add(Filter.ge("created", startDateObj));
        identityRequestQueryOptions.add(Filter.le("created", endDateObj));
        if (limit > 0 && page > 0) {
            identityRequestQueryOptions.setResultLimit(limit * page);
        }
        identityRequestQueryOptions.addOrdering("created", false);
        // type - eventName
        // targetId - Identity ID
        // name - IdentityRequest ID
        // created - created in unix time
        // completionStatus - IdentityRequest Status
        int elementsToSkip = limit * (page - 1);
        Iterator<Object[]> identityRequestIterator = context.search(IdentityRequest.class,
                identityRequestQueryOptions,
                "type, targetId, name, created, completionStatus, endDate, requesterDisplayName, requesterId");
        while (identityRequestIterator.hasNext()) {
            Object[] objArray = (Object[]) identityRequestIterator.next();
            if (objArray.length > 0) {

                String type = (String) objArray[0];
                String targetID = (String) objArray[1];
                String identityRequestId = (String) objArray[2];
                Date createdDate = (Date) objArray[3];
                CompletionStatus identityRequestCompletionStatus = (CompletionStatus) objArray[4];
                Date endDateidr = (Date) objArray[5];
                String requesterDisplayName = (String) objArray[6];
                String requesterId = (String) objArray[7];
                String identityRequestCompletionStatusString = "";
                if (identityRequestCompletionStatus != null) {
                    identityRequestCompletionStatusString = identityRequestCompletionStatus.toString();
                } else {
                    identityRequestCompletionStatusString = "Pending";
                }

                String taskResultId = "";
                String taskResultName = "";
                String taskResultStatus = "";

                String targetName = "";
                String targetDisplayName = "";

                IdentityRequest identityRequest = context.getObjectByName(IdentityRequest.class, identityRequestId);
                Attributes<String, Object> identityRequestAttributes = identityRequest.getAttributes();
                if (identityRequestAttributes.containsKey("taskResultId")) {
                    taskResultId = identityRequestAttributes.getString("taskResultId");
                }
                context.decache(identityRequest);

                if (Util.isNotNullOrEmpty(taskResultId)) {
                    TaskResult eventTaskResult = context.getObjectById(TaskResult.class, taskResultId);
                    if (eventTaskResult != null) {
                        taskResultName = eventTaskResult.getName();
                        if (eventTaskResult.getCompletionStatus() != null) {
                            taskResultStatus = eventTaskResult.getCompletionStatus().toString();
                        } else {
                            taskResultStatus = "Running";
                        }
                        context.decache(eventTaskResult);
                    }
                }

                if (Util.isNotNullOrEmpty(targetID)) {
                    Identity targetIdentity = context.getObjectById(Identity.class, targetID);
                    if (targetIdentity != null) {
                        targetName = targetIdentity.getName();
                        targetDisplayName = targetIdentity.getDisplayableName();
                        context.decache(targetIdentity);
                    }
                }

                String dataToHash = type + targetID + String.valueOf(createdDate.getTime())
                        + (endDateidr != null ? String.valueOf(endDateidr.getTime()) : "");
                String uniqueHash = "";
                try {
                    MessageDigest digest = MessageDigest.getInstance("SHA-1");
                    byte[] hash = digest.digest(dataToHash.getBytes());
                    uniqueHash = DatatypeConverter.printHexBinary(hash);
                } catch (NoSuchAlgorithmException e) {
                    log.error("Error generating SHA-1 hash: " + e.getMessage());
                    e.printStackTrace();
                }

                Map<String, String> oneEvent = new HashMap<String, String>();
                oneEvent.put("type", type);
                oneEvent.put("targetName", targetName);
                oneEvent.put("targetDisplayName", targetDisplayName);
                oneEvent.put("requesterDisplayName", requesterDisplayName);
                oneEvent.put("requesterId", requesterId);
                oneEvent.put("targetId", targetID);
                oneEvent.put("identityRequestId", identityRequestId);
                oneEvent.put("createdDate", sdf.format(createdDate));
                oneEvent.put("endDate", (endDateidr != null ? sdf.format(endDateidr) : ""));
                oneEvent.put("uniqueHash", uniqueHash);
                oneEvent.put("taskResultId", taskResultId);
                oneEvent.put("taskResultName", taskResultName);
                oneEvent.put("taskResultStatus", taskResultStatus);
                oneEvent.put("identityRequestCompletionStatusString", identityRequestCompletionStatusString);

                events.add(oneEvent);
            }
        }
        log.debug("events Size - " + events.size());
        log.debug("elementsToSkip - " + elementsToSkip);
        if (events.size() >= elementsToSkip && limit > 0 && page > 0) {
            events = events.subList(elementsToSkip, events.size());
            if (events.size() > limit) {
                events = events.subList(0, limit);
            }
        }
        returnMap.put(event, events);
        return returnMap;
    }
}