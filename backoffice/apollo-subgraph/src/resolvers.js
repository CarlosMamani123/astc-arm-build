import { Kind, print, visit } from 'graphql';
import { callBackoffice } from './client.js';

const ARG_TYPE_OVERRIDES = {
  editUser: { EditUserInput: 'EditUserInputInput' }
};

function getLeafTypeName(typeObj) {
  let t = typeObj;
  while (t.kind === Kind.NON_NULL_TYPE || t.kind === Kind.LIST_TYPE) {
    t = t.type;
  }
  return t.name.value;
}

function buildForwardedQuery(info, operation, args) {
  const parentType = operation === 'mutation' ? info.schema.getMutationType() : info.schema.getQueryType();
  const fieldDef = parentType?.getFields?.()[info.fieldName];
  const argTypes = new Map();
  if (fieldDef) {
    for (const arg of fieldDef.args) {
      argTypes.set(arg.name, arg.type.toString());
    }
  }

  const usedArgs = Object.keys(args ?? {});
  const overrides = ARG_TYPE_OVERRIDES[info.fieldName] ?? {};
  const varDefs = usedArgs.map((name) => {
    const raw = argTypes.get(name) ?? 'String';
    const replaced = overrides[raw] ?? raw;
    return `$${name}: ${replaced}`;
  });
  const varRefs = usedArgs.map((name) => `${name}: $${name}`);

  const fieldNode = info.fieldNodes[0];
  const selectionSet = fieldNode.selectionSet
    ? visit(fieldNode.selectionSet, {
        Field(node) {
          if (node.name.value === '__typename') {
            return null;
          }
          return undefined;
        }
      })
    : null;

  const selection = selectionSet ? ` ${print(selectionSet)}` : '';
  const fieldCall = varRefs.length ? `${info.fieldName}(${varRefs.join(', ')})` : info.fieldName;
  return `${operation}${varDefs.length ? `(${varDefs.join(', ')})` : ''} { ${fieldCall}${selection} }`;
}

function forward(operation) {
  return async (_, args, contextValue, info) => {
    const query = buildForwardedQuery(info, operation, args);
    const data = await callBackoffice(query, args, contextValue.token);
    return data ? data[info.fieldName] : null;
  };
}

export const resolvers = {
  Query: {
    listUsers: forward('query'),
    getUser: forward('query'),
    getallnotifications: forward('query'),
    getNotificationsByCategory: forward('query'),
    getUnreadCount: forward('query'),
    getUnreadCountByCategory: forward('query')
  },

  Mutation: {
    createUser: forward('mutation'),
    editUser: forward('mutation'),
    deleteUser: forward('mutation'),
    registerDevice: forward('mutation'),
    readNotification: forward('mutation'),
    deleteNotification: forward('mutation'),
    markAllNotificationsAsRead: forward('mutation')
  },

  UserOutput: {
    __resolveReference(reference) {
      return { id: reference.id };
    }
  }
};