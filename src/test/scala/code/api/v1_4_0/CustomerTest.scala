package code.api.v1_4_0

import java.util.Date

import code.api.DefaultUsers
import code.api.v1_4_0.JSONFactory1_4_0.CustomerJson
import code.customer.{CustomerFaceImage, Customer, CustomerProvider}
import code.model.{User, BankId}
import net.liftweb.common.{Full, Empty, Box}
import dispatch._
import code.api.util.APIUtil.OAuth._

class CustomerTest extends V140ServerSetup with DefaultUsers {

  val mockBankId = BankId("testBank1")

  case class MockFaceImage(date : Date, url : String) extends CustomerFaceImage

  case class MockCustomer(customerId: String, number: String, mobileNumber: String,
                          legalName: String, email: String,
                          faceImage: MockFaceImage, dateOfBirth: Date,
                          relationshipStatus: String, dependents: Int,
                          dobOfDependents: List[Date], highestEducationAttained: String,
                          employmentStatus: String, kycStatus: Boolean, lastOkDate: Date) extends Customer

  val format = new java.text.SimpleDateFormat("dd/MM/yyyy")
  val mockCustomerFaceImage = MockFaceImage(new Date(1234000), "http://example.com/image1")
  val mockCustomer = MockCustomer("uuid-aisuhuiuhuikjhfd", "123", "3939", "Bob", "bob@example.com", mockCustomerFaceImage, new Date(1234000), "Single", 3, List(format.parse("30/03/2012"), format.parse("30/03/2012"), format.parse("30/03/2014")), "Bachelor’s Degree", "Employed", true, new Date(1234000))

  object MockedCustomerProvider extends CustomerProvider {

    override def checkCustomerNumberAvailable(bankId : BankId, customerNumber : String) = {true}

    override def getCustomer(bankId: BankId, user: User): Box[Customer] = {
      if(bankId == mockBankId) Full(mockCustomer)
      else Empty
    }

    override def getUser(bankId: BankId, customerId: String): Box[User] = Empty
    override def addCustomer(bankId : BankId, user : User, number : String, legalName : String, mobileNumber : String, email : String, faceImage: CustomerFaceImage,
                             dateOfBirth: Date,
                             relationshipStatus: String,
                             dependents: Int,
                             dobOfDependents: List[Date],
                             highestEducationAttained: String,
                             employmentStatus: String,
                             kycStatus: Boolean,
                             lastOkDate: Date) :  Box[Customer] = Empty
  }

  override def beforeAll() {
    super.beforeAll()
    //use the mock connector
    Customer.customerProvider.default.set(MockedCustomerProvider)
  }

  override def afterAll() {
    super.afterAll()
    //reset the default connector
    Customer.customerProvider.default.set(Customer.buildOne)
  }


  feature("Getting a bank's customer info of the current user") {

    scenario("There is no current user") {
      Given("There is no logged in user")

      When("We make the request")
      val request = (v1_4Request / "banks" / mockBankId.value / "customer").GET
      val response = makeGetRequest(request)

      Then("We should get a 400")
      response.code should equal(400)
    }

    scenario("There is a user, but the bank in questions has no customer info") {
      Given("The bank in question has no customer info")
      val testBank = BankId("test-bank")
      val user = obpuser1

      Customer.customerProvider.vend.getCustomer(testBank, user).isEmpty should equal(true)

      When("We make the request")
      //TODO: need stronger link between obpuser1 and user1
      val request = (v1_4Request / "banks" / testBank.value / "customer").GET <@(user1)
      val response = makeGetRequest(request)

      Then("We should get a 400")
      response.code should equal(400)
    }

    scenario("There is a user, and the bank in questions has customer info for that user") {
      Given("The bank in question has customer info")
      val testBank = mockBankId
      val user = obpuser1

      Customer.customerProvider.vend.getCustomer(testBank, user).isEmpty should equal(false)

      When("We make the request")
      //TODO: need stronger link between obpuser1 and user1
      val request = (v1_4Request / "banks" / testBank.value / "customer").GET <@(user1)
      val response = makeGetRequest(request)

      Then("We should get a 200")
      response.code should equal(200)

      And("We should get the right information back")

      val info = response.body.extract[CustomerJson]
      val received = MockCustomer(
        info.customer_id,
        info.customer_number,
        info.mobile_phone_number,
        info.legal_name,
        info.email,
        MockFaceImage(info.face_image.date, info.face_image.url),
        info.date_of_birth,
        info.relationship_status,
        info.dependants,
        info.dob_of_dependants,
        info.highest_education_attained,
        info.employment_status,
        info.kyc_status,
        info.last_ok_date)

      received should equal(mockCustomer)
    }

  }


}
