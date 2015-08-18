/*
 * Copyright 2011 PEAK6 Investments, L.P.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package peak6.util

import grizzled.slf4j.Logging

object CrashingThread {
  def start(name: Option[String] = None,
            daemon: Boolean = false)(target: => Unit): CrashingThread = {
    val th = new CrashingThread(name, target)
    th.setDaemon(daemon)
    th.start()
    th
  }
}

class CrashingThread(name: Option[String], target: => Unit)
  extends Thread() with Logging {

  final override def run() {
    name foreach {
      setName
    }
    try
      target
    catch {
      case e: Throwable =>
        logger.error("Unhandled exception:\n" + e.getMessage, e)
        System.exit(1)
    }
  }
}
