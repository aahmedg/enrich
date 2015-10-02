/*
 * Copyright (c) 2012-2015 Snowplow Analytics Ltd. All rights reserved.
 *
 * This program is licensed to you under the Apache License Version 2.0,
 * and you may not use this file except in compliance with the Apache License Version 2.0.
 * You may obtain a copy of the Apache License Version 2.0 at http://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the Apache License Version 2.0 is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License Version 2.0 for the specific language governing permissions and limitations there under.
 */
package com.snowplowanalytics.snowplow.enrich
package hadoop
package bad

// Specs2
import org.specs2.mutable.Specification

// Scalding
import com.twitter.scalding._

// Cascading
import cascading.tuple.TupleEntry

// This project
import JobSpecHelpers._

/**
 * Holds the input and expected output data
 * for the test.
 */
object UnsupportedPayloadCfLinesSpec {

  val lines = Lines(
    "2012-05-24 11:35:53 DFW3 3343 99.116.172.58 GET d3gs014xn8p70.cloudfront.net /not-ice.png 200 http://www.psychicbazaar.com/2-tarot-cards/genre/all/type/all?p=5 Mozilla/5.0%20(Windows%20NT%206.1;%20WOW64;%20rv:12.0)%20Gecko/20100101%20Firefox/12.0 e=pv&page=Tarot%2520cards%2520-%2520Psychic%2520Bazaar&tid=344260&uid=288112e0a5003be2&vid=1&lang=en-US&refr=http%253A%252F%252Fwww.psychicbazaar.com%252F2-tarot-cards%252Fgenre%252Fall%252Ftype%252Fall%253Fp%253D4&f_pdf=1&f_qt=0&f_realp=0&f_wma=0&f_dir=0&f_fla=1&f_java=1&f_gears=0&f_ag=1&res=1366x768&cookie=1"
    )

  val expected = """{"line":"2012-05-24 11:35:53 DFW3 3343 99.116.172.58 GET d3gs014xn8p70.cloudfront.net /not-ice.png 200 http://www.psychicbazaar.com/2-tarot-cards/genre/all/type/all?p=5 Mozilla/5.0%20(Windows%20NT%206.1;%20WOW64;%20rv:12.0)%20Gecko/20100101%20Firefox/12.0 e=pv&page=Tarot%2520cards%2520-%2520Psychic%2520Bazaar&tid=344260&uid=288112e0a5003be2&vid=1&lang=en-US&refr=http%253A%252F%252Fwww.psychicbazaar.com%252F2-tarot-cards%252Fgenre%252Fall%252Ftype%252Fall%253Fp%253D4&f_pdf=1&f_qt=0&f_realp=0&f_wma=0&f_dir=0&f_fla=1&f_java=1&f_gears=0&f_ag=1&res=1366x768&cookie=1","errors":[{"level":"error","message":"Request path /not-ice.png does not match (/)vendor/version(/) pattern nor is a legacy /i(ce.png) request"}]}"""
}

/**
 * Integration test for the EtlJob:
 *
 * Input data _is_ in the CloudFront
 * access log format, but the fields
 * are somehow corrupted.
 */
class UnsupportedPayloadCfLinesSpec extends Specification {

  "A job which processes an input line with an unknown payload format" should {
    EtlJobSpec("cloudfront", "1", false, List("geo")).
      source(MultipleTextLineFiles("inputFolder"), UnsupportedPayloadCfLinesSpec.lines).
      sink[String](Tsv("outputFolder")){ output => 
        "not write any events" in {
          output must beEmpty
        }
      }.
      sink[TupleEntry](Tsv("exceptionsFolder")){ trap =>
        "not trap any exceptions" in {
          trap must beEmpty
        }
      }.
      sink[String](Tsv("badFolder")){ buf =>
        val json = buf.head
        "write a bad row JSON containing the input line and all errors" in {
          removeTstamp(json) must_== UnsupportedPayloadCfLinesSpec.expected
        }
      }.
      run.
      finish
  }
}
