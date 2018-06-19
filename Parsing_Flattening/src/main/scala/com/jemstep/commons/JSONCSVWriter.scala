package com.jemstep.commons

import net.liftweb.json._
import java.io.FileWriter

object JSONCSVWriter {
        case class Orion(accountId : String)
        case class AccountIntegrations(orion : Orion)
        case class Positions(assetClass : String, costBasis : String, description : String, unitPrice : String, units : String, ticker : String)
        case class Accounts(accountType: String, accountNumber: String, accountName: String, accountStatus: String,  dollarValue : String, update : String, uuid : String, accountIntegrations : AccountIntegrations, positions : Positions)
        case class Broker(accounts: List[Accounts], uuid : String)
        case class PortfolioByBroker(brokers: List[Broker])
        implicit val formats = DefaultFormats

        def writeAccountPE(jsonString : String, userId : String) : Unit = {
                //println(userId)
                val fw = new FileWriter("./output/accountpe/accountpe.csv", true)
                val brokersObj = parse (jsonString).extract[PortfolioByBroker].brokers
                brokersObj.foreach (x => x.accounts.foreach(y => { fw.write(y.accountName + "," + y.accountNumber + "," +y.accountStatus + "," + y.accountType + ","+y.dollarValue+","+userId+","+y.update+","+x.uuid+","+y.accountIntegrations.orion.accountId+","+y.uuid+"\n")}))
                fw.close()
                val fw1 = new FileWriter("./output/accountpe/holdingpe.csv", true)
                brokersObj.foreach (x => x.accounts.foreach(y => { fw1.write(y.accountIntegrations.orion.accountId+","+y.positions.assetClass+","+y.positions.costBasis+","+y.update+","+y.positions.description+",,"+y.uuid+","+y.positions.unitPrice+","+y.positions.units+","+y.positions.ticker+","+y.dollarValue+"\n")}))
                fw1.close()
        }

        case class Questions(questionId : String, answer : String)
        case class Questionnaire(questions : List[Questions])
        case class Goal(questionnaire: Questionnaire)
        def writeQuestionnariePE(jsonString : String, userId : String) : Unit = {
                val fw = new FileWriter("./output/accountpe/questionnariepe.csv", true)
                val questionnaireObj = parse (jsonString).extract[Goal].questionnaire
                questionnaireObj.questions.foreach(x => fw.write(x.questionId+","+x.answer+","+userId))
                fw.close()
        }
}
