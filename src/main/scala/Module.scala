import java.time.Clock

import com.google.inject.AbstractModule
import data.{DASUserOps, SchemeClaimOps, TokenStashOps}
import db.{DASUserDAO, SchemeClaimDAO, TokenStashDAO}

class Module extends AbstractModule {

  override def configure() = {
    // Use the system clock as the default implementation of Clock
    bind(classOf[Clock]).toInstance(Clock.systemDefaultZone)

    bind(classOf[DASUserOps]).to(classOf[DASUserDAO])
    bind(classOf[SchemeClaimOps]).to(classOf[SchemeClaimDAO])
    bind(classOf[TokenStashOps]).to(classOf[TokenStashDAO])
  }
}
