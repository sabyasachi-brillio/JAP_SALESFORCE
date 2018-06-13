import org.json4s._
import org.json4s.jackson.JsonMethods._
implicit val formats = DefaultFormats
 
case class Account(accountType: String, accountNumber: String, accountName: String, currency: String, accountStatus: String)
case class Broker(accounts: List[Account])
case class PortfolioByBroker(brokers: List[Broker])
case class accountPE(portfolioByBroker: PortfolioByBroker)
 
lotto1.extract[accountPE]