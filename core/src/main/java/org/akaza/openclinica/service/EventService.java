package org.akaza.openclinica.service;

import org.akaza.openclinica.bean.core.Status;
import org.akaza.openclinica.bean.core.SubjectEventStatus;
import org.akaza.openclinica.bean.login.UserAccountBean;
import org.akaza.openclinica.bean.managestudy.StudyBean;
import org.akaza.openclinica.bean.managestudy.StudyEventBean;
import org.akaza.openclinica.bean.managestudy.StudyEventDefinitionBean;
import org.akaza.openclinica.bean.managestudy.StudySubjectBean;
import org.akaza.openclinica.core.SessionManager;
import org.akaza.openclinica.dao.login.UserAccountDAO;
import org.akaza.openclinica.dao.managestudy.StudyDAO;
import org.akaza.openclinica.dao.managestudy.StudyEventDAO;
import org.akaza.openclinica.dao.managestudy.StudyEventDefinitionDAO;
import org.akaza.openclinica.dao.managestudy.StudySubjectDAO;
import org.akaza.openclinica.dao.submit.SubjectDAO;
import org.akaza.openclinica.exception.OpenClinicaSystemException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.HashMap;

import javax.sql.DataSource;

public class EventService implements EventServiceInterface {

    //region Finals

    protected final Logger logger = LoggerFactory.getLogger(getClass().getName());

    //endregion

    //region Members

    private SubjectDAO subjectDao;
    private StudySubjectDAO studySubjectDao;
    private UserAccountDAO userAccountDao;
    private StudyEventDefinitionDAO studyEventDefinitionDao;
    private StudyEventDAO studyEventDao;
    private StudyDAO studyDao;

    private DataSource dataSource;

    //endregion

    //region Constructors

    public EventService(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public EventService(SessionManager sessionManager) {
        this.dataSource = sessionManager.getDataSource();
    }

    //endregion

    //region Properties

    /**
     * DataSource Getter
     *
     * @return the dataSource
     */
    public DataSource getDataSource() {
        return dataSource;
    }

    /**
     * DataSource Setter
     *
     * @param dataSource the datasource to set
     */
    public void setDatasource(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * @return the subjectDao
     */
    public SubjectDAO getSubjectDao() {
        subjectDao = subjectDao != null ? subjectDao : new SubjectDAO(dataSource);
        return subjectDao;
    }

    /**
     * @return the subjectDao
     */
    public StudyDAO getStudyDao() {
        studyDao = studyDao != null ? studyDao : new StudyDAO(dataSource);
        return studyDao;
    }

    /**
     * @return the subjectDao
     */
    public StudySubjectDAO getStudySubjectDao() {
        studySubjectDao = studySubjectDao != null ? studySubjectDao : new StudySubjectDAO(dataSource);
        return studySubjectDao;
    }

    /**
     * @return the UserAccountDao
     */
    public UserAccountDAO getUserAccountDao() {
        userAccountDao = userAccountDao != null ? userAccountDao : new UserAccountDAO(dataSource);
        return userAccountDao;
    }

    /**
     * @return the StudyEventDefinitionDao
     */
    public StudyEventDefinitionDAO getStudyEventDefinitionDao() {
        studyEventDefinitionDao = studyEventDefinitionDao != null ? studyEventDefinitionDao : new StudyEventDefinitionDAO(dataSource);
        return studyEventDefinitionDao;
    }

    /**
     * @return the StudyEventDao
     */
    public StudyEventDAO getStudyEventDao() {
        studyEventDao = studyEventDao != null ? studyEventDao : new StudyEventDAO(dataSource);
        return studyEventDao;
    }

    //endregion

    //region Methods

    public HashMap<String, String> scheduleEvent(
            UserAccountBean user,
            Date startDateTime,
            Date endDateTime,
            String location,
            String studyUniqueId,
            String siteUniqueId,
            String eventDefinitionOID,
            String studySubjectId
    ) throws OpenClinicaSystemException {

        // Business Validation
        StudyBean study = getStudyDao().findByUniqueIdentifier(studyUniqueId);
        int parentStudyId = study.getId();
        if (siteUniqueId != null) {
            study = getStudyDao().findSiteByUniqueIdentifier(studyUniqueId, siteUniqueId);
        }
        StudyEventDefinitionBean studyEventDefinition = getStudyEventDefinitionDao().findByOidAndStudy(eventDefinitionOID, study.getId(), parentStudyId);
        StudySubjectBean studySubject = getStudySubjectDao().findByLabelAndStudy(studySubjectId, study);

        Integer studyEventOrdinal;
        if (canSubjectScheduleAnEvent(studyEventDefinition, studySubject)) {

            StudyEventBean studyEvent = new StudyEventBean();
            studyEvent.setStudyEventDefinitionId(studyEventDefinition.getId());
            studyEvent.setStudySubjectId(studySubject.getId());
            studyEvent.setLocation(location);
            studyEvent.setDateStarted(startDateTime);
            studyEvent.setDateEnded(endDateTime);
            studyEvent.setOwner(user);
            studyEvent.setStatus(Status.AVAILABLE);
            studyEvent.setSubjectEventStatus(SubjectEventStatus.SCHEDULED);
            studyEvent.setSampleOrdinal(getStudyEventDao().getMaxSampleOrdinal(studyEventDefinition, studySubject) + 1);
            studyEvent = (StudyEventBean) getStudyEventDao().create(studyEvent);
            studyEventOrdinal = studyEvent.getSampleOrdinal();

        }
        else {
            throw new OpenClinicaSystemException("Cannot schedule an event for this Subject");
        }

        HashMap<String, String> h = new HashMap<>();
        h.put("eventDefinitionOID", eventDefinitionOID);
        h.put("studyEventOrdinal", studyEventOrdinal.toString());
        h.put("studySubjectOID", studySubject.getOid());

        return h;
    }

    private boolean canSubjectScheduleAnEvent(StudyEventDefinitionBean studyEventDefinition, StudySubjectBean studySubject) {
        return studyEventDefinition.isRepeating() ||
                getStudyEventDao().findAllByDefinitionAndSubject(studyEventDefinition, studySubject).size() <= 0;
    }

    //endregion

}