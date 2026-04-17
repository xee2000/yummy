import {apiClient} from './client';

export interface ServerUser {
  id: number;
  tossId: string;
  name: string;
  height: number | null;
  weight: number | null;
  age: number | null;
}

export const userApi = {
  /** tossId로 유저 조회 — 없으면 null */
  findByTossId: (tossId: string): Promise<ServerUser> =>
    apiClient.get(`/users/toss/${tossId}`),

  /** 최초 가입 */
  create: (data: {tossId: string; name: string; height?: number; weight?: number; age?: number}): Promise<ServerUser> =>
    apiClient.post('/users', data),

  /** 유저 정보 수정 */
  update: (id: number, data: {name?: string; height?: number; weight?: number; age?: number}): Promise<ServerUser> =>
    apiClient.patch(`/users/${id}`, data),
};
