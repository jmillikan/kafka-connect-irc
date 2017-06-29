/**
 * Copyright © 2016 Christopher Matta (chris.matta@gmail.com)
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
package com.github.cjmatta.kafka.connect.irc;

import com.google.common.collect.ImmutableMap;
import org.apache.kafka.common.config.ConfigException;
import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.data.Struct;
import org.apache.kafka.connect.errors.ConnectException;
import org.apache.kafka.connect.sink.SinkRecord;
import org.apache.kafka.connect.sink.SinkTask;
import com.github.cjmatta.kafka.connect.irc.util.KafkaBotNameGenerator;
import org.schwering.irc.lib.IRCConnection;
import org.schwering.irc.lib.IRCEventAdapter;
import org.schwering.irc.lib.IRCUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class IrcSinkTask extends SinkTask {
  static final Logger log = LoggerFactory.getLogger(IrcSinkTask.class);

  private static final String TIMESTAMP_FIELD = "timestamp";
  private static final String CHANNEL_FIELD = "channel";
  private static final Schema KEY_SCHEMA = Schema.STRING_SCHEMA;

  IrcSourceTaskConfig config;

  private String ircServer;
  private List<String> channels;
  private int ircPort;
  private String ircBotName;
  private String ircBotAuth;

  private String topic;

  private IRCConnection connection;

  @Override
  public String version() {
    return VersionUtil.getVersion();
  }

  @Override
  public void start(Map<String, String> props) {
    try {
      config = new IrcSourceTaskConfig(props);
      ircServer = config.getIrcServer();
      ircPort = config.getIrcServerPort();
      ircBotName = config.getIrcBotName();
      ircBotAuth = config.getIrcBotAuth();
      
      channels = config.getIrcChannels();
      topic = config.getKafkaTopic();

      this.connection = new IRCConnection(ircServer, new int[]{ircPort}, ircBotAuth, ircBotName, ircBotName, ircBotName);
      //      this.connection.addIRCEventListener(new IrcMessageEvent());
      this.connection.setEncoding("UTF-8");
      this.connection.setPong(true);
      this.connection.setColors(false);

      if(log.isInfoEnabled()) {
        log.info("Connecting to server: {}", config.getIrcServer());
      }
      try {
        this.connection.connect();
      } catch (IOException e) {
        throw new ConnectException("Unable to connect to server: " + this.ircServer);
      }

      for (String channel : config.getIrcChannels()) {
        if(log.isInfoEnabled()) {
          log.info("Joining channel: {}", channel);
        }
        try {
          this.connection.doJoin(channel);
        } catch (Exception e) {
          throw new ConnectException("Problem joining channel " + channel);
        }

      }
    } catch (ConfigException e) {
      throw new ConfigException("IrcSourceTask couldn't start due to configuration exception: ", e);
    }
  }

  @Override
  public void put(Collection<SinkRecord> records) {
      // this.connection.doPrivmsg("#jmillikan", "Test message - TODO");
      for ( SinkRecord record : records ) {
	  Object v = record.value();

	  // The right way to do this is probably to use AvroConverter.fromConnectData,
	  // set up a schema in code and check it against record.valueSchema...
	  // For now, die horribly if we get something weird.

	  // Expected schema:
	  // {"type":"record","name":"twitchsink","fields":[{"name":"createdat","type":"long"}, {"name":"channel","type":"string"}, {"name":"message","type":"string"}]}

	  if(v instanceof Struct) {
	      Struct s = (Struct) v;
	      String channel = s.getString("channel");
	      String message = s.getString("message");
	      
	      // Assume that incoming request stream is valid e.g. already rate-limited etc.
	      this.connection.doPrivmsg(channel, message);
	  }
	  else {
	      log.warn("Unexpected SinkRecord value: " + v.toString());
	  }
      }
  }

  @Override
  public void stop() {
    //TODO: Do whatever is required to stop your task.
    for(String channel: this.channels){
      this.connection.doPart(channel);
    }

    this.connection.interrupt();

    try {
      this.connection.join();
    } catch (InterruptedException e) {
      throw new RuntimeException("Problem shutting down IRC connection: " + this.ircServer + ":" + this.ircPort);
    }

    if(this.connection.isAlive()) {
      throw new RuntimeException("Could not shut down IRC connection!");
    }

    // this.queue.clear();

  }

//   class IrcMessageEvent extends IRCEventAdapter {
//     @Override
//     public void onPrivmsg(String channel, IRCUser user, String message) {
// //      Message date
//       Date timestamp = new Date();

//       IrcUser ircUser = new IrcUser(user.getNick(), user.getUsername(), user.getHost());

//       IrcMessage ircMessage = new IrcMessage(
//           timestamp,
//           channel,
//           ircUser,
//           message);
// // Since "resuming" isn't really a thing you can do with IRC these are simply empty maps.
//       Map<String, ?> srcOffset = ImmutableMap.of();
//       Map<String, ?> srcPartition = ImmutableMap.of();

//       // SourceRecord record = new SourceRecord(
//       //     srcPartition,
//       //     srcOffset,
//       //     topic,
//       //     KEY_SCHEMA,
//       //     channel,
//       //     IrcMessage.SCHEMA,
//       //     ircMessage);

//       // queue.offer(record);

//     }
//   }
}
