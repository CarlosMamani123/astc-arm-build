import { parse } from 'graphql';

export const typeDefs = parse(/* GraphQL */ `
  extend schema
    @link(url: "https://specs.apollo.dev/federation/v2.3", import: ["@key", "@shareable"])

  type Query {
    listUsers: [UserOutput]
    getUser(id: String): UserOutput
    getallnotifications(userId: String, page: Int, size: Int): PaginationResponse
    getNotificationsByCategory(userId: String, category: String, page: Int, size: Int): PaginationResponse
    getUnreadCount(userId: String, category: String): Int
    getUnreadCountByCategory(userId: String): [InAppNotificationCategoryUnreadCount]
  }

  type Mutation {
    createUser(input: CreateUserInput): CreateUserResponse
    editUser(id: String, input: EditUserInput): UserOutput
    deleteUser(id: String): Boolean
    registerDevice(userId: String, token: String, platform: String): Boolean
    readNotification(notificationId: String, userId: String): InAppNotification
    deleteNotification(notificationId: String, userId: String): Boolean
    markAllNotificationsAsRead(userId: String): Boolean
  }

  # =========================
  # USER ENTITY (FEDERATION)
  # =========================
  type UserOutput @key(fields: "id") {
    id: ID!
    username: String @shareable
    email: String @shareable
    firstName: String @shareable
    lastName: String @shareable
    fullName: String @shareable
    phone: String @shareable
    roleId: String @shareable
    avatarUrl: String @shareable
    roleCode: String @shareable
    roleName: String @shareable
  }

  type CreateUserResponse @shareable {
    id: ID!
    username: String
    email: String
    firstName: String
    lastName: String
    phone: String
    roleId: String
    avatarUrl: String
    roleCode: String
    roleName: String
  }

  # =========================
  # NOTIFICATIONS
  # =========================
  type InAppNotification @shareable {
    id: String
    userId: String
    title: String
    body: String
    notificationCode: String
    icon: String
    color: String
    actionLink: String
    read: Boolean!
    createdAt: String
  }

  type PaginationResponse @shareable {
    items: [InAppNotification]
    page: Int!
    size: Int!
    total: Int!
  }

  type InAppNotificationCategoryUnreadCount @shareable {
    category: String
    count: Int!
  }

  # =========================
  # INPUTS
  # =========================
  input CreateUserInput {
    username: String
    email: String
    password: String
    firstName: String
    lastName: String
    phone: String
    roleId: String
    avatarUrl: String
  }

  input EditUserInput {
    username: String
    email: String
    password: String
    firstName: String
    lastName: String
    phone: String
    roleId: String
    avatarUrl: String
  }
`);