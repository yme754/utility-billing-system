import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import { AuthService } from './auth';

@Injectable({
  providedIn: 'root'
})
export class BillingService {
  private apiUrl = 'http://localhost:8080/bills'; 
  private tariffUrl = 'http://localhost:8080/consumers/tariffs'; 

  constructor(private http: HttpClient, private authService: AuthService) {}

  private getHeaders() {
    const token = this.authService.getToken();
    return new HttpHeaders().set('Authorization', `Bearer ${token}`);
  }

  getAdminStats(): Observable<any> {
    return this.http.get(`${this.apiUrl}/admin/stats`, { headers: this.getHeaders() });
  }

  getAllBills(): Observable<any[]> {
    return this.http.get<any[]>(`${this.apiUrl}/all`, { headers: this.getHeaders() });
  }

  getAllTariffs(): Observable<any[]> {
    return this.http.get<any[]>(`${this.tariffUrl}`, { headers: this.getHeaders() });
  }

  createTariff(tariff: any): Observable<any> {
    return this.http.post(`${this.tariffUrl}`, tariff, { headers: this.getHeaders() });
  }

  updateTariff(id: string, tariff: any): Observable<any> {
    return this.http.put(`${this.tariffUrl}/${id}`, tariff, { headers: this.getHeaders() });
  }

  deleteTariff(id: string): Observable<any> {
    return this.http.delete(`${this.tariffUrl}/${id}`, { headers: this.getHeaders() });
  }
  
  generateBill(billRequest: any): Observable<any> {
    return this.http.post(`${this.apiUrl}/generate`, billRequest, { headers: this.getHeaders() });
  }

  generateBills(month: number, year: number): Observable<any> {
    return this.http.post(
      `${this.apiUrl}/generate?month=${month}&year=${year}`, 
      {}, 
      { headers: this.getHeaders() }
    );
  }
}