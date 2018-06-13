//package com.jemstep.apiexample

import java.io._
import java.util._
import scala.util.control._
import com.sforce.async._
import com.sforce.soap.partner.DescribeSObjectResult
import com.sforce.soap.partner.PartnerConnection
import com.sforce.ws.ConnectionException
import com.sforce.ws.ConnectorConfig
//import BulkExample._
//remove if not needed
import scala.collection.JavaConversions._

object BulkExample {

  def main(args: Array[String]) {
    val example = new BulkExample()
   
  }
}

class BulkExample {

  /**
   * Creates a Bulk API job and uploads batches for a CSV file.
   */
  def runSample(sobjectType: String, 
      userName: String, 
      password: String, 
      sampleFileName: String) {
    val connection = getBulkConnection(userName, password)
    val job = createJob(sobjectType, connection)
    val batchInfoList = createBatchesFromCSVFile(connection, job, sampleFileName)
    closeJob(connection, job.getId)
    awaitCompletion(connection, job, batchInfoList)
    checkResults(connection, job, batchInfoList)
  }

  def retrieveBulkData(userName: String, 
      password: String, 
      objectName: String, 
      query: String, 
      fileName: String, 
      environment: String) {
    val start = System.currentTimeMillis()
    val bulkconnection = getBulkConnection(userName, password)
    val job = createJob(objectName, bulkconnection)
    val byteArrayInputStream = getData(query, bulkconnection, job)
    closeJob(bulkconnection, job.getId)
    println("FileName::" + fileName)
    println(byteArrayInputStream.toString)
    val end = System.currentTimeMillis()
    val total = start - end
    println("total:" + total)
    val seconds = ((total / 1000) % 60).toInt
    val minutes = ((total / (1000 * 60)) % 60).toInt
    println("Time To Execute is::" + minutes + ":" + seconds)
  }

  def getData(query: String, bulkConnection: BulkConnection, job: JobInfo): ByteArrayInputStream = {
    val inputStream: ByteArrayInputStream = null
    try {
      var info: BatchInfo = null
      val bout = new ByteArrayInputStream(query.getBytes)
      info = bulkConnection.createBatchFromStream(job, bout)
      var queryResults: Array[String] = null
      var list: QueryResultList = null
      var count = 0
      val loop = new Breaks;
      loop.breakable{
      for (i <- 0 until 10000) {
        count += 1
        Thread.sleep(if (i == 0) 30 * 1000 else 30 * 1000)
        info = bulkConnection.getBatchInfo(job.getId, info.getId)
        println("-------------- Info ----------" + info)
        if (info.getState == BatchStateEnum.Completed) {
          list = bulkConnection.getQueryResultList(job.getId, info.getId)
          queryResults = list.getResult
          loop.break
        } else if (info.getState == BatchStateEnum.Failed) {
          println("-------------- failed ----------" + info)
          loop.break
        } else {
          println("-------------- waiting ----------" + info)
        }
      }
      }
      println("count::" + count)
      println("QueryResultList::" + list.toString)
      if (queryResults != null) {
        for (resultId <- queryResults) {
          val inputStream1 = bulkConnection.getQueryResultStream(job.getId, info.getId, resultId)
          println("-------------- InputSream 1 ----------" + inputStream1.toString)
        }
      }
    } catch {
      case aae: AsyncApiException => aae.printStackTrace()
      case ie: InterruptedException => ie.printStackTrace()
    }
    inputStream
  }

  /**
   * Gets the results of the operation and checks for errors.
   */
  private def checkResults(connection: BulkConnection, job: JobInfo, batchInfoList: List[BatchInfo]) {
    for (b <- batchInfoList) {
      val rdr = new CSVReader(connection.getBatchResultStream(job.getId, b.getId))
      val resultHeader = rdr.nextRecord()
      val resultCols = resultHeader.size
      var row: List[String] = null
      while ((row = rdr.nextRecord()) != null) {
        val resultInfo = new HashMap[String, String]()
        for (i <- 0 until resultCols) {
          resultInfo.put(resultHeader.get(i), row.get(i))
        }
        val success = resultInfo.get("Success")
        val created = resultInfo.get("Created")
        val id = resultInfo.get("Id")
        val error = resultInfo.get("Error")
        if (success=="Success" && created == "Created") {
          println("Created row with id " + id)
        } else if (success!= "Success") {
          println("Failed with error: " + error)
        }
      }
    }
  }

  private def closeJob(connection: BulkConnection, jobId: String) {
    val job = new JobInfo()
    job.setId(jobId)
    job.setState(JobStateEnum.Closed)
    connection.updateJob(job)
  }

  /**
   * Wait for a job to complete by polling the Bulk API.
   *
   * @param connection
   *            BulkConnection used to check results.
   * @param job
   *            The job awaiting completion.
   * @param batchInfoList
   *            List of batches for this job.
   * @throws AsyncApiException
   */
  private def awaitCompletion(connection: BulkConnection, job: JobInfo, batchInfoList: List[BatchInfo]) {
    var sleepTime = 0L
    val incomplete = new HashSet[String]()
    for (bi <- batchInfoList) {
      incomplete.add(bi.getId)
    }
    while (!incomplete.isEmpty) {
      try {
        Thread.sleep(sleepTime)
      } catch {
        case e: InterruptedException => 
      }
      println("Awaiting results..." + incomplete.size)
      sleepTime = 10000L
      val statusList = connection.getBatchInfoList(job.getId).getBatchInfo
      for (b <- statusList if b.getState == BatchStateEnum.Completed || b.getState == BatchStateEnum.Failed
          if incomplete.remove(b.getId)) {
        println("BATCH STATUS:\n" + b)
      }
    }
  }

  /**
   * Create a new job using the Bulk API.
   *
   * @param sobjectType
   *            The object type being loaded, such as "Account"
   * @param connection
   *            BulkConnection used to create the new job.
   * @return The JobInfo for the new job.
   * @throws AsyncApiException
   */
  private def createJob(sobjectType: String, connection: BulkConnection): JobInfo = {
    var job = new JobInfo()
    job.setObject(sobjectType)
    job.setOperation(OperationEnum.insert)
    job.setContentType(ContentType.CSV)
    job = connection.createJob(job)
    println(job)
    job
  }

  /**
   * Create the BulkConnection used to call Bulk API operations.
   */
  private def getBulkConnection(userName: String, password: String): BulkConnection = {
    val partnerConfig = new ConnectorConfig()
    partnerConfig.setUsername(userName)
    partnerConfig.setPassword(password)
    partnerConfig.setAuthEndpoint("https://login.salesforce.com/services/Soap/u/42.0")
    new PartnerConnection(partnerConfig)
    val config = new ConnectorConfig()
    config.setSessionId(partnerConfig.getSessionId)
    val soapEndpoint = partnerConfig.getServiceEndpoint
    val apiVersion = "42.0"
    val restEndpoint = soapEndpoint.substring(0, soapEndpoint.indexOf("Soap/")) + 
      "async/" + 
      apiVersion
    config.setRestEndpoint(restEndpoint)
    config.setCompression(true)
    config.setTraceMessage(false)
    val connection = new BulkConnection(config)
    connection
  }

  /**
   * Create and upload batches using a CSV file.
   * The file into the appropriate size batch files.
   *
   * @param connection
   *            Connection to use for creating batches
   * @param jobInfo
   *            Job associated with new batches
   * @param csvFileName
   *            The source file for batch data
   */
  private def createBatchesFromCSVFile(connection: BulkConnection, jobInfo: JobInfo, csvFileName: String): List[BatchInfo] = {
    val batchInfos = new ArrayList[BatchInfo]()
    val rdr = new BufferedReader(new InputStreamReader(new FileInputStream(csvFileName)))
    val headerBytes = (rdr.readLine() + "\n").getBytes("UTF-8")
    val headerBytesLength = headerBytes.length
    val tmpFile = File.createTempFile("bulkAPIInsert", ".csv")
    try {
      var tmpOut = new FileOutputStream(tmpFile)
      val maxBytesPerBatch = 10000000
      val maxRowsPerBatch = 10000
      var currentBytes = 0
      var currentLines = 0
      var nextLine: String = null
     // while ((nextLine = rdr.readLine()) != null) {
      while (Stream.continually(rdr.readLine()).takeWhile(_ != null) != null) {
        val bytes = (nextLine + "\n").getBytes("UTF-8")
        if (currentBytes + bytes.length > maxBytesPerBatch || currentLines > maxRowsPerBatch) {
          createBatch(tmpOut, tmpFile, batchInfos, connection, jobInfo)
          currentBytes = 0
          currentLines = 0
        }
        if (currentBytes == 0) {
          tmpOut = new FileOutputStream(tmpFile)
          tmpOut.write(headerBytes)
          currentBytes = headerBytesLength
          currentLines = 1
        }
        tmpOut.write(bytes)
        currentBytes += bytes.length
        currentLines += 1
      }
      if (currentLines > 1) {
        createBatch(tmpOut, tmpFile, batchInfos, connection, jobInfo)
      }
    } finally {
      tmpFile.delete()
    }
    batchInfos
  }

  /**
   * Create a batch by uploading the contents of the file.
   * This closes the output stream.
   *
   * @param tmpOut
   *            The output stream used to write the CSV data for a single batch.
   * @param tmpFile
   *            The file associated with the above stream.
   * @param batchInfos
   *            The batch info for the newly created batch is added to this list.
   * @param connection
   *            The BulkConnection used to create the new batch.
   * @param jobInfo
   *            The JobInfo associated with the new batch.
   */
  private def createBatch(tmpOut: FileOutputStream, 
      tmpFile: File, 
      batchInfos: List[BatchInfo], 
      connection: BulkConnection, 
      jobInfo: JobInfo) {
    tmpOut.flush()
    tmpOut.close()
    val tmpInputStream = new FileInputStream(tmpFile)
    try {
      val batchInfo = connection.createBatchFromStream(jobInfo, tmpInputStream)
      println(batchInfo)
      batchInfos.add(batchInfo)
    } finally {
      tmpInputStream.close()
    }
  }
}
