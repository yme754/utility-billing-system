import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { AuthService } from './auth';
import { map } from 'rxjs/operators';

@Injectable({
  providedIn: 'root'
})
export class ConsumerService {
  private apiUrl = environment.apiUrl;

  constructor(private http: HttpClient, private authService: AuthService) {}

  private getHeaders() {
    const token = this.authService.getToken();
    return { 'Authorization': `Bearer ${token}` };
  }  

  getPublicTariffPlans(): Observable<any[]> {
    return this.http.get<any[]>(`${this.apiUrl}/consumers/tariffs`);
  }

  getConsumerCount(): Observable<number> {
    return this.http.get<number>(`${this.apiUrl}/consumers/count`);
  }

  getTotalBillsProcessed(): Observable<number> {
    return this.http.get<any[]>(`${this.apiUrl}/bills/all`).pipe(
      map(bills => bills ? bills.length : 0)
    );
  }

  getProfile(userId: string): Observable<any> {
    return this.http.get(`${this.apiUrl}/consumers/profile/${userId}`, { headers: this.getHeaders() });
  }

  createProfile(profileData: any): Observable<any> {
    return this.http.post(`${this.apiUrl}/consumers/profile`, profileData, { headers: this.getHeaders() });
  }

  updateProfile(profileData: any): Observable<any> {
    return this.http.put(`${this.apiUrl}/consumers/profile`, profileData, { headers: this.getHeaders() });
  }

  getMyConnections(consumerId: string): Observable<any[]> {
    return this.http.get<any[]>(`${this.apiUrl}/consumers/${consumerId}/connections`, { headers: this.getHeaders() });
  }

  requestConnection(connectionData: any): Observable<any> {
    return this.http.post(`${this.apiUrl}/consumers/connections`, connectionData, { headers: this.getHeaders() });
  }

  getPendingConnections(): Observable<any[]> {
    return this.http.get<any[]>(`${this.apiUrl}/consumers/connections/pending`, { headers: this.getHeaders() });
  }

  approveConnection(connectionId: string, meterNumber: string): Observable<any> {
    const payload = { meterNumber: meterNumber };
    return this.http.put(`${this.apiUrl}/consumers/${connectionId}/approve`, payload, { headers: this.getHeaders() });
  }

  getConnectionById(id: string): Observable<any> {
    return this.http.get<any>(`${this.apiUrl}/consumers/connections/${id}`, { headers: this.getHeaders() });
  }

  getAllConnections(): Observable<any[]> {
    return this.http.get<any[]>(`${this.apiUrl}/consumers/connections`, { headers: this.getHeaders() });
  }

  updateConnectionStatus(id: string, status: string) {
    return this.http.put(`${this.apiUrl}/consumers/connections/${id}/status`, { status }, { headers: this.getHeaders() });
  }

  getBillsByConnection(connectionId: string): Observable<any[]> {
    return this.http.get<any[]>(`${this.apiUrl}/bills/my-bills/${connectionId}`, { headers: this.getHeaders() });
  }

  getAllTariffPlans(): Observable<any[]> {
     return this.http.get<any[]>(`${this.apiUrl}/consumers/tariffs`, { headers: this.getHeaders() });
  }
}