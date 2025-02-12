'use server'

let currentUsers = 0;

export async function getCurrentUsers() {
  return currentUsers;
}

export async function incrementUsers() {
  currentUsers++;
  return currentUsers;
}

export async function decrementUsers() {
  currentUsers = Math.max(0, currentUsers - 1);
  return currentUsers;
}

