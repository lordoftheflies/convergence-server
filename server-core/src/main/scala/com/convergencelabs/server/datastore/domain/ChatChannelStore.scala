package com.convergencelabs.server.datastore.domain

import java.util.ArrayList
import java.util.{ List => JavaList }
import java.util.{ Set => JavaSet }

import scala.collection.JavaConverters.asJavaCollectionConverter
import scala.collection.JavaConverters.collectionAsScalaIterableConverter
import scala.collection.JavaConverters.mapAsJavaMapConverter
import scala.collection.JavaConverters.seqAsJavaListConverter
import scala.collection.JavaConverters.asScalaSetConverter
import scala.collection.JavaConverters.setAsJavaSetConverter
import scala.util.Failure
import scala.util.Success
import scala.util.Try

import com.convergencelabs.server.datastore.AbstractDatabasePersistence
import com.convergencelabs.server.datastore.DatabaseProvider
import com.convergencelabs.server.datastore.QueryUtil
import com.orientechnologies.orient.core.db.record.OTrackedList
import com.orientechnologies.orient.core.id.ORID
import com.orientechnologies.orient.core.index.OCompositeKey
import com.orientechnologies.orient.core.metadata.schema.OType
import com.orientechnologies.orient.core.record.impl.ODocument
import com.orientechnologies.orient.core.sql.OCommandSQL
import com.convergencelabs.server.datastore.domain.ChatChannelStore._

import grizzled.slf4j.Logging
import com.convergencelabs.server.datastore.EntityNotFoundException
import java.time.Instant
import java.util.Date
import java.util.HashSet
import com.orientechnologies.orient.core.storage.ORecordDuplicatedException
import com.convergencelabs.server.datastore.DuplicateValueExcpetion

case class ChatChannel(
  id: String,
  channelType: String,
  created: Instant,
  isPrivate: Boolean,
  name: String,
  topic: String)

case class ChatChannelInfo(
  id: String,
  channelType: String,
  created: Instant,
  isPrivate: Boolean,
  name: String,
  topic: String,
  members: Set[String],
  lastEventNo: Long,
  lastEventTime: Instant)

sealed trait ChatChannelEvent {
  val eventNo: Long
  val channel: String
  val user: String
  val timestamp: Instant
}

case class ChatCreatedEvent(
  eventNo: Long,
  channel: String,
  user: String,
  timestamp: Instant,
  name: String,
  topic: String,
  members: Set[String]) extends ChatChannelEvent

case class ChatMessageEvent(
  eventNo: Long,
  channel: String,
  user: String,
  timestamp: Instant,
  message: String) extends ChatChannelEvent

case class ChatUserJoinedEvent(
  eventNo: Long,
  channel: String,
  user: String,
  timestamp: Instant) extends ChatChannelEvent

case class ChatUserLeftEvent(
  eventNo: Long,
  channel: String,
  user: String,
  timestamp: Instant) extends ChatChannelEvent

case class ChatUserAddedEvent(
  eventNo: Long,
  channel: String,
  user: String,
  timestamp: Instant,
  userAdded: String) extends ChatChannelEvent

case class ChatUserRemovedEvent(
  eventNo: Long,
  channel: String,
  user: String,
  timestamp: Instant,
  userRemoved: String) extends ChatChannelEvent

case class ChatNameChangedEvent(
  eventNo: Long,
  channel: String,
  user: String,
  timestamp: Instant,
  name: String) extends ChatChannelEvent

case class ChatTopicChangedEvent(
  eventNo: Long,
  channel: String,
  user: String,
  timestamp: Instant,
  topic: String) extends ChatChannelEvent

case class ChatChannelMember(channel: String, user: String, seen: Long)

object ChatChannelStore {

  object Classes {
    val ChatChannel = "ChatChannel"
    val ChatChannelEvent = "ChatChannelEvent"
    val ChatCreatedEvent = "ChatCreatedEvent"
    val ChatMessageEvent = "ChatMessageEvent"
    val ChatUserJoinedEvent = "ChatUserJoinedEvent"
    val ChatUserLeftEvent = "ChatUserLeftEvent"
    val ChatUserAddedEvent = "ChatUserAddedEvent"
    val ChatUserRemovedEvent = "ChatUserRemovedEvent"
    val ChatNameChangedEvent = "ChatNameChangedEvent"
    val ChatTopicChangedEvent = "ChatTopicChangedEvent"
    val ChatChannelMember = "ChatChannelMember"
  }

  object Indexes {
    val ChatChannel_Id = "ChatChannel.id"

    val ChatChannelEvent_EventNo_Channel = "ChatChannelEvent.channel_eventNo"
    val ChatChannelEvent_Channel = "ChatChannelEvent.channel"

    val ChatChannelMember_Channel_User = "ChatChannelMember_channel_user"
    val ChatChannelMember_Channel = "ChatChannelMember_channel"
  }

  object Sequences {
    val ChatChannelId = "chatChannelIdSeq"
  }

  object ChannelType extends Enumeration {
    val Group, Room, Direct = Value

    def withNameOpt(s: String): Option[Value] = values.find(_.toString.toLowerCase() == s.toLowerCase())
  }

  def channelTypeString(channelType: ChannelType.Value): String = channelType match {
    case ChannelType.Group => "group"
    case ChannelType.Room => "room"
    case ChannelType.Direct => "direct"
  }

  object Fields {
    val Id = "id"
    val Type = "type"
    val Created = "created"
    val Private = "private"
    val Name = "name"
    val Topic = "topic"
    val Members = "members"

    val EventNo = "eventNo"
    val Channel = "channel"
    val User = "user"
    val Timestamp = "timestamp"

    val Message = "message"
    val UserAdded = "userAdded"
    val UserRemoved = "userRemoved"

    val Seen = "seen"

    val Username = "username"
  }

  def docToChatChannel(doc: ODocument): ChatChannel = {
    val created: Date = doc.field(Fields.Created, OType.DATETIME)

    ChatChannel(
      doc.field(Fields.Id),
      doc.field(Fields.Type),
      created.toInstant(),
      doc.field(Fields.Private),
      doc.field(Fields.Name),
      doc.field(Fields.Topic))
  }

  def chatChannelToDoc(chatChannel: ChatChannel): ODocument = {
    val doc = new ODocument(Classes.ChatChannel)
    doc.field(Fields.Id, chatChannel.id)
    doc.field(Fields.Type, chatChannel.channelType)
    doc.field(Fields.Created, Date.from(chatChannel.created))
    doc.field(Fields.Private, chatChannel.isPrivate)
    doc.field(Fields.Name, chatChannel.name)
    doc.field(Fields.Topic, chatChannel.topic)
    doc.field(Fields.Members, new HashSet[ORID]())
    doc
  }

  def docToChatChannelEvent(doc: ODocument): ChatChannelEvent = {
    val eventNo: Long = doc.field(Fields.EventNo)
    val channel: String = doc.field("channel.id")
    val user: String = doc.field("user.username")
    val timestamp: Date = doc.field(Fields.Timestamp, OType.DATETIME)

    val className = doc.getClassName

    className match {
      case Classes.ChatCreatedEvent =>
        val name: String = doc.field(Fields.Name)
        val topic: String = doc.field(Fields.Topic)
        val members: JavaSet[ODocument] = doc.field("members")
        val usernames: Set[String] = members.asScala.toSet.map { doc: ODocument => doc.field("username").asInstanceOf[String] }
        ChatCreatedEvent(eventNo, channel, user, timestamp.toInstant(), name, topic, usernames)
      case Classes.ChatMessageEvent =>
        val message: String = doc.field(Fields.Message)
        ChatMessageEvent(eventNo, channel, user, timestamp.toInstant(), message)
      case Classes.ChatUserJoinedEvent =>
        ChatUserJoinedEvent(eventNo, channel, user, timestamp.toInstant())
      case Classes.ChatUserLeftEvent =>
        ChatUserLeftEvent(eventNo, channel, user, timestamp.toInstant())
      case Classes.ChatUserAddedEvent =>
        val userAdded: String = doc.field("userAdded.username")
        ChatUserAddedEvent(eventNo, channel, user, timestamp.toInstant(), userAdded)
      case Classes.ChatUserRemovedEvent =>
        val userRemoved: String = doc.field("userRemoved.username")
        ChatUserRemovedEvent(eventNo, channel, user, timestamp.toInstant(), userRemoved)
      case Classes.ChatTopicChangedEvent =>
        val topic: String = doc.field(Fields.Topic)
        ChatTopicChangedEvent(eventNo, channel, user, timestamp.toInstant(), topic)
      case Classes.ChatNameChangedEvent =>
        val name: String = doc.field(Fields.Name)
        ChatNameChangedEvent(eventNo, channel, user, timestamp.toInstant(), name)
      case _ =>
        throw new IllegalArgumentException(s"Unknown Chat Channel Event class name: ${className}")
    }
  }
}

class ChatChannelStore(private[this] val dbProvider: DatabaseProvider) extends AbstractDatabasePersistence(dbProvider) with Logging {

  def getChatChannelInfo(channelId: String): Try[ChatChannelInfo] = tryWithDb { db =>
    val queryString =
      """SELECT 
        |  channel.id as id, channel.type as type, channel.created as created, 
        |  channel.private as private, channel.name as name, channel.topic as topic,
        |  channel.members as members, eventNo, timestamp
        |  FROM ChatChannelEvent 
        |  WHERE channel.id = :channelId
        |  ORDER BY eventNo Desc
        |  LIMIT 1""".stripMargin

    val params = Map("channelId" -> channelId)
    val result = QueryUtil.lookupMandatoryDocument(queryString, params, db)
    result.map {
      doc =>
        val id: String = doc.field("id")
        val channelType: String = doc.field("type")
        val created: Date = doc.field("created")
        val isPrivate: Boolean = doc.field("private")
        val name: String = doc.field("name")
        val topic: String = doc.field("topic")
        val members: JavaSet[ODocument] = doc.field("members")
        val usernames: Set[String] = members.asScala.map(member => member.field("user.username").asInstanceOf[String]).toSet
        val lastEventNo: Long = doc.field("eventNo")
        val lastEventTime: Date = doc.field("timestamp")
        ChatChannelInfo(id, channelType, created.toInstant(), isPrivate, name, topic,
          usernames, lastEventNo, lastEventTime.toInstant())
    }.get
  }

  def getChatChannel(channelId: String): Try[ChatChannel] = tryWithDb { db =>
    getChatChannelRid(channelId).map { rid =>
      docToChatChannel(rid.getRecord[ODocument])
    }.get
  }

  // FIXME: Pass in create time
  def createChatChannel(id: Option[String], channelType: ChannelType.Value, 
      isPrivate: Boolean, name: String, topic: String, members: Option[Set[String]]): Try[String] = tryWithDb { db =>
    // FIXME: return failure if addAllChatChannelMembers fails
    db.begin()
    val channelId = id.getOrElse {
      "#" + db.getMetadata.getSequenceLibrary.getSequence(Sequences.ChatChannelId).next()
    }
    val doc = chatChannelToDoc(ChatChannel(channelId, channelTypeString(channelType), Instant.now(), isPrivate, name, topic))
    db.save(doc)

    members.foreach { username =>
      addAllChatChannelMembers(channelId, username, None).get
    }
    db.commit()
    channelId
  } recoverWith {
    case e: ORecordDuplicatedException =>
      e.getIndexName match {
        case ChatChannelStore.Indexes.ChatChannel_Id =>
          Failure(DuplicateValueExcpetion("id"))
        case _ =>
          Failure(e)
      }
  }

  def updateChatChannel(channelId: String, name: Option[String], topic: Option[String]): Try[Unit] = tryWithDb { db =>
    for {
      channelRid <- getChatChannelRid(channelId)
    } yield {
      val doc = channelRid.getRecord[ODocument]
      name.foreach(doc.field(Fields.Name, _))
      topic.foreach(doc.field(Fields.Topic, _))
      doc.save()
      ()
    }
  }

  def removeChatChannel(channelId: String): Try[Unit] = tryWithDb { db =>
    for {
      channelRid <- getChatChannelRid(channelId)
    } yield {
      channelRid.getRecord[ODocument].delete()
      ()
    }
  }

  // TODO: All of the events are very similar, need to abstract some of each of these methods

  def addChatCreatedEvent(event: ChatCreatedEvent): Try[Unit] = tryWithDb { db =>
    val ChatCreatedEvent(eventNo, channel, user, timestamp, name, topic, members) = event

    val memberQueryString = "SELECT FROM User WHERE username IN :members"
    val memberParams = Map("members" -> members.asJavaCollection)
    val users: Set[ODocument] = QueryUtil.query(memberQueryString, memberParams, db).toSet

    val queryStirng =
      """INSERT INTO ChatCreatedEvent SET
        |  eventNo = :eventNo,
        |  channel = (SELECT FROM ChatChannel WHERE id = :channelId),
        |  user = (SELECT FROM User WHERE username = :username),
        |  timestamp = :timestamp,
        |  name = :name,
        |  topic = :topic,
        |  members = :members""".stripMargin
    val query = new OCommandSQL(queryStirng)
    val params = Map(
      "eventNo" -> eventNo,
      "channelId" -> channel,
      "username" -> user,
      "timestamp" -> Date.from(timestamp),
      "name" -> name,
      "topic" -> topic,
      "members" -> users.asJava)
    db.command(query).execute(params.asJava)
    ()
  }

  def addChatMessageEvent(event: ChatMessageEvent): Try[Unit] = tryWithDb { db =>
    val ChatMessageEvent(eventNo, channel, user, timestamp, message) = event
    val queryStirng =
      """INSERT INTO ChatMessageEvent SET
        |  eventNo = :eventNo,
        |  channel = (SELECT FROM ChatChannel WHERE id = :channelId),
        |  user = (SELECT FROM User WHERE username = :username),
        |  timestamp = :timestamp,
        |  message = :message""".stripMargin
    val query = new OCommandSQL(queryStirng)
    val params = Map(
      "eventNo" -> eventNo,
      "channelId" -> channel,
      "username" -> user,
      "timestamp" -> Date.from(timestamp),
      "message" -> message)
    db.command(query).execute(params.asJava)
    ()
  }

  def addChatUserJoinedEvent(event: ChatUserJoinedEvent): Try[Unit] = tryWithDb { db =>
    val ChatUserJoinedEvent(eventNo, channel, user, timestamp) = event
    val queryStirng =
      """INSERT INTO ChatUserJoinedEvent SET
        |  eventNo = :eventNo,
        |  channel = (SELECT FROM ChatChannel WHERE id = :channelId),
        |  user = (SELECT FROM User WHERE username = :username),
        |  timestamp = :timestamp""".stripMargin
    val query = new OCommandSQL(queryStirng)
    val params = Map(
      "eventNo" -> eventNo,
      "channelId" -> channel,
      "username" -> user,
      "timestamp" -> Date.from(timestamp))
    db.command(query).execute(params.asJava)
    ()
  }

  def addChatUserLeftEvent(event: ChatUserLeftEvent): Try[Unit] = tryWithDb { db =>
    val ChatUserLeftEvent(eventNo, channel, user, timestamp) = event
    val queryStirng =
      """INSERT INTO ChatUserLeftEvent SET
        |  eventNo = :eventNo,
        |  channel = (SELECT FROM ChatChannel WHERE id = :channelId),
        |  user = (SELECT FROM User WHERE username = :username),
        |  timestamp = :timestamp""".stripMargin
    val query = new OCommandSQL(queryStirng)
    val params = Map(
      "eventNo" -> eventNo,
      "channelId" -> channel,
      "username" -> user,
      "timestamp" -> Date.from(timestamp))
    db.command(query).execute(params.asJava)
    ()
  }

  def addChatUserAddedEvent(event: ChatUserAddedEvent): Try[Unit] = tryWithDb { db =>
    val ChatUserAddedEvent(eventNo, channel, user, timestamp, userAdded) = event
    val queryStirng =
      """INSERT INTO ChatUserAddedEvent SET
        |  eventNo = :eventNo,
        |  channel = (SELECT FROM ChatChannel WHERE id = :channelId),
        |  user = (SELECT FROM User WHERE username = :username),
        |  timestamp = :timestamp,
        |  userAdded = (SELECT FROM User WHERE username = :userAdded)""".stripMargin
    val query = new OCommandSQL(queryStirng)
    val params = Map(
      "eventNo" -> eventNo,
      "channelId" -> channel,
      "username" -> user,
      "timestamp" -> Date.from(timestamp),
      "userAdded" -> userAdded)
    db.command(query).execute(params.asJava)
    ()
  }

  def addChatUserRemovedEvent(event: ChatUserRemovedEvent): Try[Unit] = tryWithDb { db =>
    val ChatUserRemovedEvent(eventNo, channel, user, timestamp, userRemoved) = event
    val queryStirng =
      """INSERT INTO ChatUserRemovedEvent SET
        |  eventNo = :eventNo,
        |  channel = (SELECT FROM ChatChannel WHERE id = :channelId),
        |  user = (SELECT FROM User WHERE username = :username),
        |  timestamp = :timestamp,
        |  userRemoved = (SELECT FROM User WHERE username = :userRemoved)""".stripMargin
    val query = new OCommandSQL(queryStirng)
    val params = Map(
      "eventNo" -> eventNo,
      "channelId" -> channel,
      "username" -> user,
      "timestamp" -> Date.from(timestamp),
      "userRemoved" -> userRemoved)
    db.command(query).execute(params.asJava)
    ()
  }

  def addChatNameChangedEvent(event: ChatNameChangedEvent): Try[Unit] = tryWithDb { db =>
    val ChatNameChangedEvent(eventNo, channel, user, timestamp, name) = event
    val queryStirng =
      """INSERT INTO ChatNameChangedEvent SET
        |  eventNo = :eventNo,
        |  channel = (SELECT FROM ChatChannel WHERE id = :channelId),
        |  user = (SELECT FROM User WHERE username = :username),
        |  timestamp = :timestamp,
        |  name = :name""".stripMargin
    val query = new OCommandSQL(queryStirng)
    val params = Map(
      "eventNo" -> eventNo,
      "channelId" -> channel,
      "username" -> user,
      "timestamp" -> Date.from(timestamp),
      "name" -> name)
    db.command(query).execute(params.asJava)
    ()
  }

  def addChatTopicChangedEvent(event: ChatTopicChangedEvent): Try[Unit] = tryWithDb { db =>
    val ChatTopicChangedEvent(eventNo, channel, user, timestamp, topic) = event
    val queryStirng =
      """INSERT INTO ChatTopicChangedEvent SET
        |  eventNo = :eventNo,
        |  channel = (SELECT FROM ChatChannel WHERE id = :channelId),
        |  user = (SELECT FROM User WHERE username = :username),
        |  timestamp = :timestamp,
        |  topic = :topic""".stripMargin
    val query = new OCommandSQL(queryStirng)
    val params = Map(
      "eventNo" -> eventNo,
      "channelId" -> channel,
      "username" -> user,
      "timestamp" -> Date.from(timestamp),
      "topic" -> topic)
    db.command(query).execute(params.asJava)
    ()
  }

  def getChatChannelMembers(channelId: String): Try[Set[String]] = tryWithDb { db =>
    val queryString = "SELECT user.username as member FROM ChatChannelMember WHERE channel.id = :channelId"
    val params = Map("channelId" -> channelId)
    val result = QueryUtil.query(queryString, params, db)
    result.map { doc => doc.field("member").asInstanceOf[String] }.toSet
  }

  def addAllChatChannelMembers(channelId: String, usernames: Set[String], seen: Option[Long]): Try[Unit] = tryWithDb { db =>
    val seenVal = seen.getOrElse(0)
    for {
      channelRid <- getChatChannelRid(channelId)
    } yield {
      usernames.foreach { username =>
        for {
          userRid <- DomainUserStore.getUserRid(username, db)
        } yield {
          val doc = new ODocument(Classes.ChatChannelMember)
          doc.field(Fields.Channel, channelRid)
          doc.field(Fields.User, userRid)
          doc.field(Fields.Seen, seenVal)
          db.save(doc)

          val channelDoc = channelRid.getRecord[ODocument]
          val members: JavaSet[ORID] = channelDoc.field(Fields.Members)
          members.add(doc.getIdentity)
          channelDoc.field(Fields.Members, members)
          channelDoc.save()
          ()
        }
      }
    }
  }

  def addChatChannelMember(channelId: String, username: String, seen: Option[Long]): Try[Unit] = tryWithDb { db =>
    for {
      channelRid <- getChatChannelRid(channelId)
      userRid <- DomainUserStore.getUserRid(username, db)
    } yield {
      val doc = new ODocument(Classes.ChatChannelMember)
      doc.field(Fields.Channel, channelRid)
      doc.field(Fields.User, userRid)
      doc.field(Fields.Seen, seen.getOrElse(0))
      db.save(doc)

      val channelDoc = channelRid.getRecord[ODocument]
      val members: JavaSet[ORID] = channelDoc.field(Fields.Members)
      members.add(doc.getIdentity)
      channelDoc.field(Fields.Members, members)
      channelDoc.save()
      ()
    }
  }

  def removeChatChannelMember(channelId: String, username: String): Try[Unit] = tryWithDb { db =>
    for {
      channelRid <- getChatChannelRid(channelId)
      memberRid <- getChatChannelMemberRid(channelId, username)
    } yield {
      val channelDoc = channelRid.getRecord[ODocument]
      val members: JavaSet[ORID] = channelDoc.field(Fields.Members)
      members.remove(memberRid)
      channelDoc.field(Fields.Members, members)
      channelDoc.save()
      memberRid.getRecord[ODocument].delete()
      ()
    }
  }

  def markSeen(channelId: String, username: String, seen: Long): Try[Unit] = tryWithDb { db =>
    for {
      memberRid <- getChatChannelMemberRid(channelId, username)
    } yield {
      val doc = memberRid.getRecord[ODocument]
      doc.field(Fields.Seen, seen)
      doc.save()
      ()
    }
  }

  def getChatChannelEvents(channelId: String, offset: Option[Long], limit: Option[Long]): Try[List[ChatChannelEvent]] = tryWithDb { db =>
    val queryString = "SELECT FROM ChatChannelEvent WHERE channel.id = :channelId ORDER BY eventNo DESC"
    val limitString = limit.map(l => s"LIMIT $l").getOrElse("")
    val offsetString = offset.map(o => s"SKIP $o").getOrElse("")
    val params = Map("channelId" -> channelId)
    val result = QueryUtil.query(s"$queryString $limitString $offsetString", params, db)
    result.map { doc => docToChatChannelEvent(doc) }
  }

  def getChatChannelRid(channelId: String): Try[ORID] = tryWithDb { db =>
    QueryUtil.getRidFromIndex(Indexes.ChatChannel_Id, channelId, db).get
  }

  def getChatChannelMemberRid(channelId: String, username: String): Try[ORID] = tryWithDb { db =>
    val channelRID = getChatChannelRid(channelId).get
    val userRID = DomainUserStore.getUserRid(username, db).get
    val key = new OCompositeKey(List(userRID, channelRID).asJava)
    QueryUtil.getRidFromIndex(Indexes.ChatChannelMember_Channel_User, key, db).get
  }
}
