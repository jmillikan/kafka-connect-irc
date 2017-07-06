## Kafka Connect Twitch source/sink connect et cetera

This is a Twitch-focused fork of cjmatta's IRC source connector, with the intent of building a multi-purpose twitch bot around Kafka.

#### Running in development

```
mvn clean package
export CLASSPATH="$(find target/ -type f -name '*.jar'| grep '\-package' | tr '\n' ':')"
$CONFLUENT_HOME/bin/connect-standalone $CONFLUENT_HOME/etc/schema-registry/connect-avro-standalone.properties config/kafka-connect-twitch-source.properties
$CONFLUENT_HOME/bin/connect-standalone $CONFLUENT_HOME/etc/schema-registry/connect-avro-standalone.properties config/kafka-connect-twitch-sink.properties
```

##### License
Copyright © 2016 Christopher Matta (chris.matta@gmail.com)
Copyright © 2017 Jesse Millikan (jesse.a.millikan@gmail.com)

Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.