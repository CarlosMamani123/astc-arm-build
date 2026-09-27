Architecture:
React
Atomic Design (atoms, molecules, organisms, templates)
feature-based structure
reusable components
prepare for GraphQL integration
do NOT use MVC
Design:
use the same design system as the other roles
shared sidebar layout
same visual language, colors and components
clean, modern, corporate UI
cards for dashboard
tables for management
forms for actions
consistent UI across roles
Role:

Backoffice Admin

Screens to create:
Dashboard
Users List
User Detail
Edit User
Block User modal
GraphQL operations:
ListUsers
GetUserDetail
CreateUser
EditUser
DeleteUser
Requirements:

Dashboard must show admin summary cards such as:

active users
total registered users
users by role
blocked users

Dashboard must include quick actions:

Create User
Manage Users

Users List must include:

search
filters
table
row actions
pagination

User Detail must include:

personal data
role
contact information
user state

Edit User must include:

editable form fields
save and cancel actions

Block User must be a reusable confirmation modal

Delete User should be a confirmation-ready action flow

Behavior:
must focus on administrative user and role management
must support create, edit, delete and block flows
must reuse components from the shared layout
include loading, empty and error states
Data requirements:
Generate realistic mock data following GraphQL response structure
Always use a "data" root object
Match the exact fields expected for each operation
Include multiple records for tables (at least 2–5)
Include different user roles and user states
Include pagination fields where needed: page, size, total
Do not use placeholder values like "John Doe"
Use realistic business data (emails, dates, statuses, etc.)
Mock data must look like a real backend response