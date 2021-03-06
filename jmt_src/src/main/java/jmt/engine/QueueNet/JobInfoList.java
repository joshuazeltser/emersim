/**
 * Copyright (C) 2016, Laboratorio di Valutazione delle Prestazioni - Politecnico di Milano

 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA
 */
package jmt.engine.QueueNet;

import java.util.List;

import jmt.engine.dataAnalysis.InverseMeasure;
import jmt.engine.dataAnalysis.Measure;

/**
 * <p><b>Name:</b> JobInfoList</p> 
 * <p><b>Description:</b> 
 * A JobInfoList is a list used to update raw sampled performance counters of a node or node section
 * and optionally (depending on the implementation) to hold the list of all the jobs currently in the given section.
 * </p>
 * <p><b>Date:</b> 18/giu/2009
 * <b>Time:</b> 17:00:44</p>
 * @author Bertoli Marco
 * @version 1.0
 * 
 * Modified by Ashanka (May 2010): 
 * Patch: Multi-Sink Perf. Index 
 * Description: Added new Performance index for the capturing the 
 * 				1. global response time (ResponseTime per Sink)
 *              2. global throughput (Throughput per Sink)
 *              each sink per class.
 */
public interface JobInfoList {

	/** Gets list size.
	 * @return Number of job info object in the list.
	 * @throws jmt.common.exception.NetException
	 */
	public abstract int size() throws jmt.common.exception.NetException;

	/**
	 * Returns the number of jobs of a specific job class in the list.
	 * @param jobClass Job class to look for.
	 * @return Number of jobs of a specified job class.
	 */
	public abstract int size(JobClass jobClass) throws jmt.common.exception.NetException;

	/** Gets the number of jobs added to the list.
	 * @return Arrived Jobs.
	 */
	public abstract int getJobsIn();

	/** Gets the number of jobs of a specific job class added to the list.
	 * @param jobClass Job class to look for.
	 * @return Arrived jobs of a specific job class.
	 */
	public abstract int getJobsInPerClass(JobClass jobClass);

	/** Gets the number of jobs removed from the list.
	 * @return Departed Jobs.
	 */
	public abstract int getJobsOut();

	/** Gets the number of jobs of a specific job class removed from the list.
	 * @param jobClass Job class to look for.
	 * @return Departed jobs of a specific job class.
	 */
	public abstract int getJobsOutPerClass(JobClass jobClass);

	/** Gets busy time.
	 * @return Busy time.
	 * @throws jmt.common.exception.NetException
	 */
	public abstract double getBusyTime() throws jmt.common.exception.NetException;

	/** Gets busy time for job class.
	 * @return Busy time for job class.
	 * @throws jmt.common.exception.NetException
	 */
	public abstract double getBusyTimePerClass(JobClass jobClass) throws jmt.common.exception.NetException;

	/** Gets time of the last job arrived.
	 * @return Time of last job arrived.
	 */
	public abstract double getLastJobInTime();

	/** Gets time of the last job arrived of a specified job class.
	 * @return Time of last job arrived of a specified job class.
	 */
	public abstract double getLastJobInTimePerClass(JobClass jobClass);

	/** Gets time of the last job was dropped.
	 * @return Time of last job departed.
	 */
	public abstract double getLastJobOutTime();

	/** Gets time of the last job was dropped.
	 * @return Time of last job departed.
	 */
	public abstract double getLastJobDropTime();

	/** Gets time of the last job departed of a specified job class.
	 * @return Time of last job departed of a specified job class.
	 */
	public abstract double getLastJobOutTimePerClass(JobClass jobClass);

	/** Gets time of the last job dropped of a specified job class.
	 * @return Time of last job dropped of a specified job class.
	 */
	public abstract double getLastJobDropTimePerClass(JobClass jobClass);

	/** Gets time of the last modify of the list.
	 * @return Time of last modify of the list.
	 */
	public abstract double getLastModifyTime();

	/** Gets time of the last modify of the list for a specified job class.
	 * @return Time of the last modify of the list for a specified job class.
	 */
	public abstract double getLastModifyTimePerClass(JobClass jobClass);

	/** Looks for an information job object which references to a specific job.
	 * @param job The specified job.
	 * @return JobInfo object which references to the specified job, null otherwise.
	 */
	public abstract JobInfo lookFor(Job job) throws jmt.common.exception.NetException;

	/**
	 * Gets the job info list
	 */
	public abstract List<JobInfo> getJobList();

	/** Adds a new job info to the list.
	 * @param jobInfo Reference to the job info to be added.
	 * @return True if the job has been added (True if <tt>Save</tt> property is true,
	 * otherwise no list was created by the constructor)
	 */
	public abstract boolean add(JobInfo jobInfo);

	/** Adds a new job info to the top of the list.
	 * @param jobInfo reference to job info to be added.
	 * @return True if the job has been added (True if <tt>Save</tt> property is true,
	 * otherwise no list was created by the constructor)
	 */
	public abstract boolean addFirst(JobInfo jobInfo);

	/** Adds a new job info to the bottom of the list.
	 * @param jobInfo reference to job info to be added.
	 * @return True if the job has been added (True if <tt>Save</tt> property is true,
	 * otherwise no list was created by the constructor)
	 */
	public abstract boolean addLast(JobInfo jobInfo);

	/** Adds a new job info at a specified position.
	 * @param index the specified position
	 * @param jobInfo reference to job info to be added.
	 * @return True if the job has been added (True if <tt>Save</tt> property is true,
	 * otherwise no list was created by the constructor)
	 */
	public abstract boolean add(int index, JobInfo jobInfo);

	/** Adds a new job info at a specified position.
	 * The jobs must be inserted in both general and class job lists. The
	 * position is relative to general list. In its own class job list, a job
	 * can be put at the start (head) or at the end (tail).
	 *
	 * @param index the specified position
	 * @param jobInfo reference to job info to be added.
	 * @param isClassHead if true, job will be put at the start of its own class job list (head
	 * strategy); if false, it will be put at the end (tail strategy)
	 * @return True if the job has been added (True if <tt>Save</tt> property is true,
	 * otherwise no list was created by the constructor)
	 */
	public abstract boolean add(int index, JobInfo jobInfo, boolean isClassHead);

	/** Removes a job info from the list and updates the measures related to
	 * throughput, utilization and response time.
	 * @param jobInfo reference to job info to be removed.
	 * @return True if the job has been removed (True if <tt>Save</tt> property is true,
	 * otherwise no list was created by the constructor)
	 */
	public abstract boolean remove(JobInfo jobInfo) throws jmt.common.exception.NetException;

	/** Removes a job info from the top of the list and updates the measures related to
	 * throughput, utilization and response time.
	 * @return A Job info object if it has been found, null otherwise (list
	 * empty or Save property is false)
	 */
	public abstract JobInfo removeFirst() throws jmt.common.exception.NetException;

	/** Removes a job info of a specified job class from the top of the list and updates the measures related to
	 * throughput, utilization and response time.
	 * @return A Job info object if it has been found, null otherwise (list
	 * empty or Save property is false)
	 */
	public abstract JobInfo removeFirst(JobClass jobClass) throws jmt.common.exception.NetException;

	/** Removes a job info from the bottom of the list and updates the measures related to
	 * throughput, utilization and response time.
	 * @return A Job info object if it has been found, null otherwise (list
	 * empty or Save property is false)
	 */
	public abstract JobInfo removeLast() throws jmt.common.exception.NetException;

	/** Removes a job info of a specified job class from the bottom of the list  and updates the measures related to
	 * throughput, utilization and response time.
	 * @return A Job info object if it has been found, null otherwise (list
	 * empty or Save property is false)
	 */
	public abstract JobInfo removeLast(JobClass jobClass) throws jmt.common.exception.NetException;

	/** Analyzes class utilization.
	 * @param jobClass Job class to be analyzed. If null, measure will be
	 * job class independent.
	 * @param measurement Reference to a measure object.
	 */
	public abstract void analyzeUtilization(JobClass jobClass, Measure measurement);

	/** Analyzes class utilization of a join
	 * @param jobClass Job class to be analyzed. If null, measure will be
	 * job class independent.
	 * @param measurement Reference to a measure object.
	 */
	public void analyzeUtilizationJoin(JobClass jobClass, Measure measurement);

	/** Analyzes class response time.
	 * @param jobClass Job class to be analyzed. If null, measure will be
	 * job class independent.
	 * @param measurement Reference to a measure object.
	 */
	public abstract void analyzeResponseTime(JobClass jobClass, Measure measurement);

	/** Analyzes class drop rate. Bertoli Marco
	 * @param jobClass Job class to be analyzed. If null, measure will be
	 * job class independent.
	 * @param measurement Reference to a measure object.
	 */
	public abstract void analyzeDropRate(JobClass jobClass, InverseMeasure measurement);

	/**
	 * Analyzes class throughput. <br>
	 * WARNING: An InverseMeasure must be used.
	 * The aim is to save computational time: in fact it is easier to analyze throughput
	 * by passing samples which are equals to 1/X,
	 * instead of doing one division for each sample (this would make simulation much slower).
	 * At the end the correct value is passed.
	 * @param jobClass Job class to be analyzed. If null, measure will be
	 * job class independent.
	 * @param measurement Reference to a InverseMeasure object.
	 */
	public abstract void analyzeThroughput(JobClass jobClass, InverseMeasure measurement);

	/** Analyzes class residence time.
	 * @param jobClass Job class to be analyzed. If null, measure will be
	 * job class independent.
	 * @param measurement Reference to a measure object.
	 */
	public abstract void analyzeResidenceTime(JobClass jobClass, Measure measurement);

	/** Analyzes list residence time.
	 * @param jobClass Job class to be analyzed. If null, measure will be
	 * job class independent.
	 * @param measurement Reference to a measure object.
	 */
	public abstract void analyzeQueueLength(JobClass jobClass, Measure measurement);

	/**
	 * Analyzes : Calculates the Throughput at each sink of the Network. 
	 * @param jobClass : Job class to be analyzed.
	 * @param measurement : Reference to the measure object.
	 */
	public abstract void analyzeThroughputPerSink(JobClass jobClass, InverseMeasure measurement);

	/**
	 * Analyzes : Calculates the Response Time from the source to 
	 * each sink of the job for each sink in the Queueing Network. 
	 * @param jobClass : Job class to be analyzed.
	 * @param measurement : Reference to the measure object.
	 */
	public abstract void analyzeResponseTimePerSink(JobClass jobClass, Measure measurement);

	/**
	 * Removes a job info from the list when a job is removed.
	 *
	 * @param jobInfo reference to the job info to be removed.
	 * @return True if the job has been removed (True if <tt>Save</tt> property is true,
	 * otherwise no list was created by the constructor)
	 */
	public abstract boolean removeJob(JobInfo jobInfo) throws jmt.common.exception.NetException;

	/**
	 * Removes a job info from the list when a job is redirected.
	 *
	 * @param jobInfo reference to the job info to be removed.
	 * @return True if the job has been removed (True if <tt>Save</tt> property is true,
	 * otherwise no list was created by the constructor)
	 */
	public abstract boolean redirectJob(JobInfo jobInfo) throws jmt.common.exception.NetException;

	/**
	 * Removes a job info from the list when a job is dropped.
	 *
	 * @param jobInfo reference to the job info to be removed.
	 * @return True if the job has been removed (True if <tt>Save</tt> property is true,
	 * otherwise no list was created by the constructor)
	 */
	public abstract boolean dropJob(JobInfo jobInfo) throws jmt.common.exception.NetException;

	/**
	 * Adds a new job info to the list when a job is produced.
	 *
	 * @param jobInfo reference to the job info to be added.
	 * @return True if the job has been removed (True if <tt>Save</tt> property is true,
	 * otherwise no list was created by the constructor)
	 */
	public abstract boolean produceJob(JobInfo jobInfo) throws jmt.common.exception.NetException;

	/**
	 * Removes a job info from the list when a job is consumed.
	 *
	 * @param jobInfo reference to the job info to be removed.
	 * @return True if the job has been removed (True if <tt>Save</tt> property is true,
	 * otherwise no list was created by the constructor)
	 */
	public abstract boolean consumeJob(JobInfo jobInfo) throws jmt.common.exception.NetException;

	/**
	 * Sets the number of servers. This parameter is used to scale utilization
	 * @param serverNumber the number of servers.
	 */
	public void setServerNumber(int serverNumber);

}
