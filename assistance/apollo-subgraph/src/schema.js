import { parse } from 'graphql';

export const typeDefs = parse(/* GraphQL */ `
  extend schema
    @link(url: "https://specs.apollo.dev/federation/v2.3", import: ["@key", "@shareable"])

  type Query {
    getDashboard(fromDate: String, toDate: String, projectId: String): DashboardOutput
    getTodayAttendance(projectId: String): AttendanceOutput
    listAttendance(
      page: Int!
      size: Int!
      projectId: String
      fromDate: String
      toDate: String
      status: String
    ): AttendancePageOutput
    listAbsences(
      page: Int!
      size: Int!
      projectId: String
      fromDate: String
      toDate: String
      type: String
      justified: Boolean
    ): AbsencePageOutput
    listJustifications(
      page: Int!
      size: Int!
      status: String
      fromDate: String
      toDate: String
    ): JustificationPageOutput
    getJustificationDetailPM(id: String): JustificationOutput
    listTeamJustificationsPM(
      userIds: [String]
      status: String
      fromDate: String
      toDate: String
      page: Int!
      size: Int!
    ): JustificationPageOutput
    getDashboardPMInternal(userIds: [String], projectId: String, fromDate: String, toDate: String): DashboardOutput
    listTeamAttendancePM(
      userIds: [String]
      projectId: String
      fromDate: String
      toDate: String
      status: String
      page: Int!
      size: Int!
    ): AttendancePageOutput
  }

  type Mutation {
    registerAttendance(input: RegisterAttendanceInput): RegisterAttendanceOutput
    checkOut(projectId: String, photoUrl: String, latitude: Float, longitude: Float): RegisterAttendanceOutput
    saveJustification(input: SaveJustificationInputInput): SaveJustificationOutput
    editJustification(id: String, input: SaveJustificationInputInput): SaveJustificationOutput
    submitJustification(id: String): SaveJustificationOutput
    deleteJustification(id: String): Boolean
    justifyAbsence(input: JustifyAbsenceInputInput): JustifyAbsenceOutput
    requestObservationPM(justificationId: String, comment: String, reviewedBy: String): JustificationOutput
    approveJustificationPM(justificationId: String, comment: String, reviewedBy: String): JustificationOutput
    rejectJustificationPM(justificationId: String, comment: String, reviewedBy: String): JustificationOutput
  }

  type DashboardOutput @shareable {
    totalAttendances: Int
    totalAbsences: Int
    pendingJustifications: Int
  }

  type AttendanceOutput @key(fields: "id") {
    id: ID!
    userId: String @shareable
    projectId: String @shareable
    date: String @shareable
    checkIn: String @shareable
    checkOut: String @shareable
    status: String @shareable
    latitude: Float @shareable
    longitude: Float @shareable
    photoUrl: String @shareable
  }

  type AttendancePageOutput @shareable {
    items: [AttendanceOutput]
    page: Int!
    size: Int!
    total: Int!
  }

  type RegisterAttendanceOutput {
    id: String
    userId: String
    projectId: String
    date: String
    checkIn: String
    checkOut: String
    status: String
    photoUrl: String
    latitude: Float
    longitude: Float
  }

  type AbsenceOutput @key(fields: "id") {
    id: String
    userId: String @shareable
    projectId: String @shareable
    date: String @shareable
    type: String @shareable
    justified: Boolean @shareable
  }

  type AbsencePageOutput @shareable {
    items: [AbsenceOutput]
    page: Int!
    size: Int!
    total: Int!
  }

  type JustifyAbsenceOutput {
    absenceId: String
    valid: Boolean
  }

  type JustificationOutput @key(fields: "id") {
    id: ID!
    absenceId: String @shareable
    userId: String @shareable
    description: String @shareable
    documentUrl: String @shareable
    status: String @shareable
    comment: String @shareable
    submittedAt: String @shareable
    reviewedAt: String @shareable
    reviewedBy: String @shareable
    absenceDate: String @shareable
    absenceType: String @shareable
    history: [JustificationHistoryOutput] @shareable
  }

  type JustificationHistoryOutput @shareable {
    id: String
    justificationId: String
    previousStatus: String
    newStatus: String
    comment: String
    changedBy: String
    changedAt: String
  }

  type JustificationPageOutput @shareable {
    items: [JustificationOutput]
    page: Int!
    size: Int!
    total: Int!
  }

  type SaveJustificationOutput {
    id: String
    status: String
    submittedAt: String
  }

  input RegisterAttendanceInput {
    latitude: Float
    longitude: Float
    photoUrl: String
    projectId: String
    replace: Boolean
  }

  input SaveJustificationInputInput {
    absenceId: String
    description: String
    documentUrl: String
    fileBase64: String
  }

  input JustifyAbsenceInputInput {
    absenceId: String
  }
`);