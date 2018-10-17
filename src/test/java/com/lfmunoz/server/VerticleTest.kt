package com.lfmunoz.server

////////////////////////////////////////////////////////////////////////////////
// import
////////////////////////////////////////////////////////////////////////////////
import com.lfmunoz.infiniteIterator
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.*
import org.assertj.core.api.Assertions.*
////////////////////////////////////////////////////////////////////////////////
// VerticleTest
////////////////////////////////////////////////////////////////////////////////
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class VerticleTest {

    //uut
   // private lateinit var ssh: SSHExecuteKotlin

    // constant fields
   // private val hostname = "10.37.120.52"
   // private val username = "centos"
  //  private val password = "centos"


   // @BeforeEach
    //fun init() {
      //  ssh = SSHExecuteKotlin()
    //}
    ////////////////////////////////////////////////////////////////////////////////
    // Test Cases
    ////////////////////////////////////////////////////////////////////////////////
    @Test
    fun `ip iterator`() {
        val ipList = listOf("0.0.0.0", "0.0.0.1", "0.0.0.2")
        val ipItr = infiniteIterator(ipList)
        assertThat(ipItr.hasNext()).isTrue()
        assertThat(ipItr.next()).isEqualTo(ipList[0])
        assertThat(ipItr.next()).isEqualTo(ipList[1])
        assertThat(ipItr.next()).isEqualTo(ipList[2])
        assertThat(ipItr.next()).isEqualTo(ipList[0])
        assertThat(ipItr.next()).isEqualTo(ipList[1])
        assertThat(ipItr.next()).isEqualTo(ipList[2])
        assertThat(ipItr.hasNext()).isTrue()
    }


} // end of VerticleTest